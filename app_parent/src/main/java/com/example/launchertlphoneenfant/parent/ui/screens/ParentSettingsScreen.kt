package com.example.launchertlphoneenfant.parent.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.launchertlphoneenfant.parent.BuildConfig
import com.example.launchertlphoneenfant.parent.data.ApkInstaller
import com.example.launchertlphoneenfant.parent.data.AppUpdateInfo
import com.example.launchertlphoneenfant.parent.data.UpdateChecker
import com.example.launchertlphoneenfant.parent.data.UpdateDialog
import kotlinx.coroutines.launch

@Composable
fun ParentSettingsScreen(
    familyCode: String,
    onFamilyCodeChange: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val updateChecker = remember { UpdateChecker() }
    val apkInstaller = remember { ApkInstaller(context) }

    var updateInfoState by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var downloadProgressState by remember { mutableStateOf<Int?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Appairage Code Famille
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Appairage Famille", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = familyCode,
                    onValueChange = onFamilyCodeChange,
                    label = { Text("Code Famille") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        // Personnalisation Thème & Interface
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Personnalisation de l'interface", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Personnalisez les couleurs et le style visuel de l'application de contrôle parental.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { Toast.makeText(context, "Thèmes par défaut actifs", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Changer le thème de couleurs")
                }
            }
        }

        // Vérification de mise à jour
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mise à jour de l'application", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    enabled = !isCheckingUpdate,
                    onClick = {
                        isCheckingUpdate = true
                        coroutineScope.launch {
                            val info = updateChecker.checkCloudUpdate("parent")
                            isCheckingUpdate = false
                            if (info.hasUpdate) {
                                updateInfoState = info
                            } else {
                                Toast.makeText(context, "Vous utilisez déjà la dernière version !", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isCheckingUpdate) "Vérification..." else "Vérifier les Mises à Jour")
                }
            }
        }

        // À propos & Version
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("À propos", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Version v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})", fontWeight = FontWeight.Bold)
                Text("Développé pour la famille avec amour 💕", fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.Gray)
            }
        }
    }

    // Pop-up de mise à jour
    updateInfoState?.let { updateInfo ->
        UpdateDialog(
            updateInfo = updateInfo,
            downloadProgress = downloadProgressState,
            onDismiss = { updateInfoState = null },
            onDownloadClick = { downloadUrl ->
                coroutineScope.launch {
                    apkInstaller.downloadAndInstallApk(downloadUrl) { progress ->
                        downloadProgressState = progress
                    }
                }
            }
        )
    }
}
