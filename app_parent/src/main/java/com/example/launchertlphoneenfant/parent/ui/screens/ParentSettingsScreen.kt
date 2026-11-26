package com.example.launchertlphoneenfant.parent.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
    val apkInstaller = remember { ApkInstaller(context) }
    val database = remember { FirebaseDatabase.getInstance() }

    val prefs = remember { context.getSharedPreferences("parent_theme_prefs", Context.MODE_PRIVATE) }
    var selectedThemeMode by remember { mutableStateOf(prefs.getString("theme_mode", "Système") ?: "Système") }
    var selectedColorPalette by remember { mutableStateOf(prefs.getString("color_palette", "Orange Chaud") ?: "Orange Chaud") }

    var updateInfoState by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var downloadProgressState by remember { mutableStateOf<Int?>(null) }
    var showUpToDateDialog by remember { mutableStateOf(false) }

    var childName by remember { mutableStateOf("Mon Champion") }
    var parentPin by remember { mutableStateOf("1234") }

    var familyCodeInput by remember(familyCode) { mutableStateOf(familyCode) }
    var showFamilyCodeDialog by remember { mutableStateOf(false) }

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
        // 1. Appairage Famille (avec bouton Valider et confirmation)
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
                    value = familyCodeInput,
                    onValueChange = { familyCodeInput = it },
                    label = { Text("Code Famille") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (familyCodeInput.isNotBlank() && familyCodeInput != familyCode) {
                            showFamilyCodeDialog = true
                        } else {
                            Toast.makeText(context, "Le code famille est identique ou vide", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Valider le Code Famille")
                }
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

        // 4. Personnalisation Thème & Couleurs
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

        // 5. Partager l'application avec un QR Code
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

        // 6. Vérification de mise à jour (Forçage garanti instantané pour test)
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
                    onClick = {
                        // Déclenchement instantané garanti de la pop-up de mise à jour v2.1
                        updateInfoState = AppUpdateInfo(
                            hasUpdate = true,
                            latestVersionName = "2.1",
                            latestVersionCode = 999,
                            releaseNotes = "🎉 Nouvelle version majeure v2.1 disponible !\n\n- Amélioration de l'appui long pour déplacer les tâches\n- Corrections et fluidité accrue",
                            downloadUrl = "https://github.com/bouriez28-wq/Launcher-enfant-android/releases/download/latest-dev/app_parent-debug.apk"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Vérifier les Mises à Jour")
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

    // 0. Confirmation Code Famille
    if (showFamilyCodeDialog) {
        AlertDialog(
            onDismissRequest = { showFamilyCodeDialog = false },
            title = { Text("Changer de Code Famille ?") },
            text = { Text("Voulez-vous vraiment valider et basculer vers le code famille \"$familyCodeInput\" ?") },
            confirmButton = {
                Button(
                    onClick = {
                        onFamilyCodeChange(familyCodeInput.trim().uppercase())
                        showFamilyCodeDialog = false
                        Toast.makeText(context, "Code famille mis à jour avec succès !", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Oui, valider")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    familyCodeInput = familyCode
                    showFamilyCodeDialog = false
                }) { Text("Annuler") }
            }
        )
    }

    // 1. Changer le prénom de l'enfant (avec confirmation)
    if (showChildNameDialog) {
        AlertDialog(
            onDismissRequest = { showChildNameDialog = false },
            title = { Text("Changer le prénom de l'enfant") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newChildNameInput,
                        onValueChange = { newChildNameInput = it },
                        label = { Text("Nouveau prénom") },
                        singleLine = true
                    )
                }
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
                    Text("Valider")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChildNameDialog = false }) { Text("Annuler") }
            }
        )
    }

    // 2. Changer le code PIN parent (avec confirmation)
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Changer le code PIN parent") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { newPinInput = it },
                        label = { Text("Nouveau code PIN (4 chiffres)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
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
                    Text("Valider")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) { Text("Annuler") }
            }
        )
    }

    // 3. Dialogue Thèmes & Palettes de couleurs (avec pastilles de couleur surlignées)
    if (showThemeDialog) {
        val palettes = listOf(
            Triple("Orange Chaud", "Chaud et dynamique", Color(0xFFFF6D00)),
            Triple("Bleu Océan", "Frais et professionnel", Color(0xFF0288D1)),
            Triple("Vert Forêt", "Apaisant et naturel", Color(0xFF2E7D32)),
            Triple("Violet Magique", "Créatif et ludique", Color(0xFF7B1FA2)),
            Triple("Rose Doux", "Chaleureux et tendre", Color(0xFFC2185B)),
            Triple("Rouge Passion", "Énergique", Color(0xFFD32F2F)),
            Triple("Jaune Soleil", "Lumineux", Color(0xFFFBC02D)),
            Triple("Gris Sobre", "Professionnel et neutre", Color(0xFF455A64))
        )

        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Thème & Palette de Couleurs") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().height(350.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                    Text("Palette de couleurs disponibles :", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        palettes.forEach { (paletteName, _, color) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedColorPalette = paletteName }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedColorPalette == paletteName,
                                    onClick = { selectedColorPalette = paletteName }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(color, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    paletteName,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedColorPalette == paletteName) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedColorPalette == paletteName) color else MaterialTheme.colorScheme.onSurface
                                )
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

    // Dialogue "Votre application est à jour"
    if (showUpToDateDialog) {
        AlertDialog(
            onDismissRequest = { showUpToDateDialog = false },
            title = { Text("✅ Application à jour") },
            text = { Text("Votre application est déjà à jour !\nVersion v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})") },
            confirmButton = {
                Button(onClick = { showUpToDateDialog = false }) {
                    Text("OK")
                }
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
