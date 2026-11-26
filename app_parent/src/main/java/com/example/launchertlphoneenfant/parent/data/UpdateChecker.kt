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
    val latestVersionName: String = "1.1",
    val latestVersionCode: Int = 2,
    val releaseNotes: String = "",
    val downloadUrl: String = ""
)

class UpdateChecker {

    suspend fun checkCloudUpdate(familyCode: String = "FAM-8492", appType: String = "parent"): AppUpdateInfo = withContext(Dispatchers.IO) {
        try {
            val db = FirebaseDatabase.getInstance()

            // On teste directement le chemin global app_updates/parent qui est le plus direct
            val ref = db.getReference("app_updates").child(appType)
            val snapshot = ref.get().await()

            Log.d("UpdateChecker", "Checking path: ${ref.toString()}, exists: ${snapshot.exists()}, value: ${snapshot.value}")

            if (snapshot.exists()) {
                val rawCode = snapshot.child("latestVersionCode").value
                val latestCode = when (rawCode) {
                    is Number -> rawCode.toInt()
                    is String -> rawCode.toIntOrNull() ?: 2
                    else -> 2
                }

                val rawName = (snapshot.child("latestVersionName").value as? String) ?: "1.1"
                val latestName = if (rawName.isBlank() || rawName == "latest-dev") "1.1" else rawName

                val notes = (snapshot.child("releaseNotes").value as? String) ?: "Nouvelle mise à jour disponible."
                val url = (snapshot.child("downloadUrl").value as? String) ?: ""

                val currentCode = com.example.launchertlphoneenfant.parent.BuildConfig.VERSION_CODE
                // Pour forcer le test tant qu'on est en phase de test, on met hasUpdate = true si le snapshot existe
                val hasUpdate = latestCode > currentCode || latestCode >= 99999
                Log.d("UpdateChecker", "latestCode: $latestCode, currentCode: $currentCode, hasUpdate: $hasUpdate")

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
