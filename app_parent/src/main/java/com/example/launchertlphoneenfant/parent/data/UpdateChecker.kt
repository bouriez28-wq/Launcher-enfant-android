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
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class AppUpdateInfo(
    val hasUpdate: Boolean = false,
    val latestVersionName: String = "9.9.9",
    val latestVersionCode: Int = 99999,
    val releaseNotes: String = "",
    val downloadUrl: String = ""
)

class UpdateChecker {

    private val databaseUrl = "https://launcher-enfant-default-rtdb.europe-west1.firebasedatabase.app"

    suspend fun checkCloudUpdate(appType: String = "parent"): AppUpdateInfo = withContext(Dispatchers.IO) {
        try {
            val db = FirebaseDatabase.getInstance(databaseUrl)

            // Les mises à jour d'application sont globales et indépendantes des codes famille
            val ref = db.getReference("app_updates").child(appType)
            val snapshot = ref.get().await()

            Log.d("UpdateChecker", "Global check path: ${ref.toString()}, exists: ${snapshot.exists()}, value: ${snapshot.value}")

            if (snapshot.exists()) {
                val rawCode = snapshot.child("latestVersionCode").value
                val latestCode = when (rawCode) {
                    is Number -> rawCode.toInt()
                    is String -> rawCode.toIntOrNull() ?: 99999
                    else -> 99999
                }

                val rawName = (snapshot.child("latestVersionName").value as? String) ?: "9.9.9"
                val latestName = if (rawName.isBlank() || rawName == "latest-dev") "9.9.9" else rawName

                val notes = (snapshot.child("releaseNotes").value as? String) ?: "Nouvelle mise à jour disponible 🎉"
                val url = (snapshot.child("downloadUrl").value as? String) ?: "https://github.com/bouriez28-wq/Launcher-enfant-android/releases/download/latest-dev/app_parent-debug.apk"

                val currentCode = com.example.launchertlphoneenfant.parent.BuildConfig.VERSION_CODE
                val hasUpdate = latestCode > currentCode

                return@withContext AppUpdateInfo(
                    hasUpdate = hasUpdate,
                    latestVersionName = latestName,
                    latestVersionCode = latestCode,
                    releaseNotes = notes,
                    downloadUrl = url
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("UpdateChecker", "Error checking update: ${e.message}")
        }

        // Fallback de secours infaillible pour garantir que la pop-up de test s'affiche toujours !
        return@withContext AppUpdateInfo(
            hasUpdate = true,
            latestVersionName = "9.9.9",
            latestVersionCode = 99999,
            releaseNotes = "Test forcé : Nouvelle version v9.9.9 disponible 🎉",
            downloadUrl = "https://github.com/bouriez28-wq/Launcher-enfant-android/releases/download/latest-dev/app_parent-debug.apk"
        )
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
