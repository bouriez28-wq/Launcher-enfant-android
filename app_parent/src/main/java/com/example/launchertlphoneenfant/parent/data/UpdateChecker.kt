package com.example.launchertlphoneenfant.parent.data

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val hasUpdate: Boolean,
    val latestVersionName: String,
    val latestVersionCode: Int,
    val releaseNotes: String,
    val downloadUrl: String
)

class UpdateChecker {

    suspend fun checkCloudUpdate(appType: String = "parent"): AppUpdateInfo = withContext(Dispatchers.IO) {
        val urls = listOf(
            "https://launcher-enfant.web.app/version.json",
            "https://raw.githubusercontent.com/bouriez28-wq/Launcher-enfant-android/main/public/version.json"
        )
        for (urlStr in urls) {
            val result = tryFetchFromUrl(urlStr)
            if (result != null) {
                return@withContext result
            }
        }

        return@withContext AppUpdateInfo(
            hasUpdate = false,
            latestVersionName = "1.1",
            latestVersionCode = 2,
            releaseNotes = "",
            downloadUrl = ""
        )
    }

    private fun tryFetchFromUrl(versionJsonUrl: String): AppUpdateInfo? {
        try {
            val url = URL(versionJsonUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.instanceFollowRedirects = true

            val code = connection.responseCode
            if (code in 200..299) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                val rawText = response.toString().trim()
                val obj: JSONObject = if (rawText.startsWith("[")) {
                    val arr = JSONArray(rawText)
                    if (arr.length() > 0) arr.getJSONObject(0) else return null
                } else {
                    JSONObject(rawText)
                }

                val dataObj = obj.optJSONObject("data") ?: obj

                val latestVersionCode = dataObj.optInt("latestVersionCode", 2)
                val latestVersionName = dataObj.optString("latestVersionName", "1.1")
                val releaseNotes = dataObj.optString("releaseNotes", "Nouvelle version disponible.")
                val downloadUrl = dataObj.optString("downloadUrl", "")

                val currentVersionCode = com.example.launchertlphoneenfant.parent.BuildConfig.VERSION_CODE
                // Comparaison stricte normale : pas de boucle infinie une fois mis à jour
                val hasUpdate = latestVersionCode > currentVersionCode

                Log.d("UpdateChecker", "Successfully fetched from $versionJsonUrl: code=$latestVersionCode, current=$currentVersionCode, hasUpdate=$hasUpdate")

                return AppUpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersionName = latestVersionName,
                    latestVersionCode = latestVersionCode,
                    releaseNotes = releaseNotes,
                    downloadUrl = downloadUrl
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}

@Composable
fun UpdateDialog(
    updateInfo: AppUpdateInfo,
    downloadProgress: Int? = null,
    onDismiss: () -> Unit,
    onDownloadClick: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = "Nouvelle mise à jour disponible", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "Version v${updateInfo.latestVersionName} (Build ${updateInfo.latestVersionCode})", fontSize = 12.sp, color = Color.Gray)
            }
        },
        text = {
            Column {
                if (updateInfo.releaseNotes.isNotBlank()) {
                    Text(text = updateInfo.releaseNotes, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (downloadProgress != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (downloadProgress in 0..99) "Téléchargement en cours... $downloadProgress%" else "Lancement de l'installation...",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { if (downloadProgress > 0) downloadProgress / 100f else 0f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (downloadProgress == null) {
                Button(onClick = { onDownloadClick(updateInfo.downloadUrl) }) {
                    Text(text = "Télécharger et installer")
                }
            }
        },
        dismissButton = {
            if (downloadProgress == null) {
                TextButton(onClick = onDismiss) {
                    Text(text = "Plus tard")
                }
            }
        }
    )
}
