package com.example.launchertlphoneenfant.parent.data

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.launchertlphoneenfant.parent.BuildConfig
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class AppUpdateInfo(
    val hasUpdate: Boolean = false,
    val latestVersionName: String = "1.0",
    val latestVersionCode: Int = 1,
    val releaseNotes: String = "",
    val downloadUrl: String = ""
)

class UpdateChecker {

    private val databaseUrl = "https://launcher-enfant-default-rtdb.europe-west1.firebasedatabase.app"

    suspend fun checkCloudUpdate(appType: String = "parent"): AppUpdateInfo = withContext(Dispatchers.IO) {
        try {
            val db = FirebaseDatabase.getInstance(databaseUrl)
            val snapshot = db.getReference("app_updates").child(appType).get().await()

            if (snapshot.exists()) {
                val latestCode = (snapshot.child("latestVersionCode").value as? Long)?.toInt() ?: 1
                val latestName = (snapshot.child("latestVersionName").value as? String) ?: "1.0"
                val notes = (snapshot.child("releaseNotes").value as? String) ?: "Nouvelle mise à jour disponible."
                val url = (snapshot.child("downloadUrl").value as? String) ?: ""

                val currentCode = BuildConfig.VERSION_CODE
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
        }
        return@withContext AppUpdateInfo(hasUpdate = false)
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
            Text(text = "🎉 Nouvelle mise à jour v${updateInfo.latestVersionName}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(text = updateInfo.releaseNotes, fontSize = 14.sp)
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
