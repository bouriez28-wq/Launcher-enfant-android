package com.example.launchertlphoneenfant.parent.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.launchertlphoneenfant.parent.BuildConfig
import com.example.launchertlphoneenfant.parent.data.ApkInstaller
import com.example.launchertlphoneenfant.parent.data.AppUpdateInfo
import com.example.launchertlphoneenfant.parent.data.UpdateChecker
import com.example.launchertlphoneenfant.parent.data.UpdateDialog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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
    val database = remember { FirebaseDatabase.getInstance() }

    val prefs = remember { context.getSharedPreferences("parent_theme_prefs", Context.MODE_PRIVATE) }
    var selectedThemeMode by remember { mutableStateOf(prefs.getString("theme_mode", "Système") ?: "Système") }
    var selectedColorPalette by remember { mutableStateOf(prefs.getString("color_palette", "Orange Chaud") ?: "Orange Chaud") }

    var updateInfoState by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var downloadProgressState by remember { mutableStateOf<Int?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }

    var childName by remember { mutableStateOf("Mon Champion") }
    var parentPin by remember { mutableStateOf("1234") }

    var showChildNameDialog by remember { mutableStateOf(false) }
    var newChildNameInput by remember { mutableStateOf("") }

    var showPinDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }

    var showQrDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }

    // Charger les infos depuis Firebase
    LaunchedEffect(familyCode) {
        if (familyCode.isNotBlank()) {
            val familyRef = database.getReference("families").child(familyCode)
            familyRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        childName = (snapshot.child("childName").value as? String) ?: "Mon Champion"
                        parentPin = (snapshot.child("parentPin").value as? String) ?: "1234"
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Appairage Famille
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

        // 2. Modifier le prénom de l'enfant
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Profil de l'Enfant", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Prénom actuel : $childName", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        newChildNameInput = childName
                        showChildNameDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Changer le prénom de l'enfant")
                }
            }
        }

        // 3. Modifier le code PIN parent
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sécurité & Code PIN Parent", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Code PIN actuel : **** (Sécurisé)", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        newPinInput = ""
                        showPinDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Changer le code PIN parent")
                }
            }
        }

        // 4. Personnalisation Thème & Couleurs (Palettes et clair/sombre)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Personnalisation de l'Interface & Couleurs", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Palette : $selectedColorPalette ($selectedThemeMode)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { showThemeDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Changer le thème et les couleurs")
                }
            }
        }

        // 5. Partager l'application avec un QR Code (Double choix Parent / Enfant)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Partager l'application", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Affichez les QR Codes de téléchargement direct pour l'App Parent ou l'App Enfant.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { showQrDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Afficher les QR Codes directs")
                }
            }
        }

        // 6. Vérification de mise à jour
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

        // 7. À propos & Version
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
                Text("Développé pour la famille avec amour 💕", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }

    // --- DIALOGS ---

    // 1. Changer le prénom de l'enfant
    if (showChildNameDialog) {
        AlertDialog(
            onDismissRequest = { showChildNameDialog = false },
            title = { Text("Changer le prénom de l'enfant") },
            text = {
                OutlinedTextField(
                    value = newChildNameInput,
                    onValueChange = { newChildNameInput = it },
                    label = { Text("Nouveau prénom") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newChildNameInput.isNotBlank()) {
                            database.getReference("families").child(familyCode).child("childName").setValue(newChildNameInput.trim())
                            showChildNameDialog = false
                            Toast.makeText(context, "Prénom mis à jour !", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChildNameDialog = false }) { Text("Annuler") }
            }
        )
    }

    // 2. Changer le code PIN parent
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Changer le code PIN parent") },
            text = {
                OutlinedTextField(
                    value = newPinInput,
                    onValueChange = { newPinInput = it },
                    label = { Text("Nouveau code PIN (4 chiffres)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinInput.length >= 4) {
                            database.getReference("families").child(familyCode).child("parentPin").setValue(newPinInput.trim())
                            showPinDialog = false
                            Toast.makeText(context, "Code PIN mis à jour !", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Le PIN doit contenir au moins 4 caractères", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("Annuler") }
            }
        )
    }

    // 3. Dialogue Thèmes & Palettes de couleurs
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Thème & Palette de Couleurs") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Mode d'affichage :", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Clair", "Sombre", "Système").forEach { mode ->
                            FilterChip(
                                selected = selectedThemeMode == mode,
                                onClick = { selectedThemeMode = mode },
                                label = { Text(mode, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Palette de couleurs :", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Orange Chaud", "Bleu Océan", "Vert Forêt", "Violet Magique", "Rose Doux").forEach { palette ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { selectedColorPalette = palette },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedColorPalette == palette,
                                    onClick = { selectedColorPalette = palette }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(palette, fontSize = 13.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        prefs.edit()
                            .putString("theme_mode", selectedThemeMode)
                            .putString("color_palette", selectedColorPalette)
                            .apply()
                        showThemeDialog = false
                        Toast.makeText(context, "Thème appliqué avec succès !", Toast.LENGTH_SHORT).show()
                        (context as? Activity)?.recreate()
                    }
                ) {
                    Text("Appliquer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Annuler") }
            }
        )
    }

    // 4. Dialogue QR Codes de téléchargement direct (Parent vs Enfant)
    if (showQrDialog) {
        val parentApkUrl = "https://github.com/bouriez28-wq/Launcher-enfant-android/releases/download/latest-dev/app_parent-debug.apk"
        val enfantApkUrl = "https://github.com/bouriez28-wq/Launcher-enfant-android/releases/download/latest-dev/app_enfant-debug.apk"

        val parentQr = "https://api.qrserver.com/v1/create-qr-code/?size=160x160&data=$parentApkUrl"
        val enfantQr = "https://api.qrserver.com/v1/create-qr-code/?size=160x160&data=$enfantApkUrl"

        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            title = { Text("📷 QR Codes de Téléchargement Direct", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().height(450.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Scannez le QR Code correspondant à l'application souhaitée :", fontSize = 13.sp, textAlign = TextAlign.Center)

                    // 1. Application Parent
                    Text("👨‍👩‍👧 1. Application Parent", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 4.dp) {
                        Image(
                            painter = rememberAsyncImagePainter(parentQr),
                            contentDescription = "QR Code Parent",
                            modifier = Modifier.size(140.dp)
                        )
                    }
                    Button(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(parentApkUrl))
                        context.startActivity(intent)
                    }) {
                        Text("Télécharger APK Parent direct")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    // 2. Application Enfant
                    Text("📱 2. Application Enfant", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 4.dp) {
                        Image(
                            painter = rememberAsyncImagePainter(enfantQr),
                            contentDescription = "QR Code Enfant",
                            modifier = Modifier.size(140.dp)
                        )
                    }
                    Button(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(enfantApkUrl))
                        context.startActivity(intent)
                    }) {
                        Text("Télécharger APK Enfant direct")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQrDialog = false }) { Text("Fermer") }
            }
        )
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
