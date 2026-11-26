package com.example.launchertlphoneenfant.parent.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun ParentHomeScreen(
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

    var totalStars by remember { mutableIntStateOf(0) }
    var childName by remember { mutableStateOf("Mon Champion") }
    var dailyLimit by remember { mutableIntStateOf(60) }
    var bonusMinutes by remember { mutableIntStateOf(0) }
    var usedMinutes by remember { mutableIntStateOf(0) }

    val database = remember { FirebaseDatabase.getInstance() }

    // Synchronisation en direct avec Firebase pour le foyer
    LaunchedEffect(familyCode) {
        if (familyCode.isNotBlank()) {
            val familyRef = database.getReference("families").child(familyCode)

            familyRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        totalStars = (snapshot.child("totalStars").value as? Long)?.toInt() ?: 0
                        childName = (snapshot.child("childName").value as? String) ?: "Mon Champion"
                        dailyLimit = (snapshot.child("dailyLimitMinutes").value as? Long)?.toInt() ?: 60
                        bonusMinutes = (snapshot.child("bonusMinutesToday").value as? Long)?.toInt() ?: 0
                        usedMinutes = (snapshot.child("usedMinutesToday").value as? Long)?.toInt() ?: 0
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
        // En-tête Résumé & Étoiles
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "🌟 Espace de $childName", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(36.dp))
                        Column {
                            Text(text = "$totalStars étoiles", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
                            Text(text = "Solde actuel de l'enfant", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val ref = database.getReference("families").child(familyCode)
                            ref.child("totalStars").setValue(totalStars + 1)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+1 Étoile ⭐")
                    }
                    Button(
                        onClick = {
                            val ref = database.getReference("families").child(familyCode)
                            ref.child("totalStars").setValue((totalStars - 1).coerceAtLeast(0))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                    ) {
                        Text("-1 Étoile")
                    }
                }
            }
        }

        // Appairage Code Famille
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Appareil Enfant Lié", fontWeight = FontWeight.Bold)
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

        // Temps d'écran & Bonus
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("⏱️ Temps d'écran aujourd'hui :", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$usedMinutes min consommées / ${dailyLimit + bonusMinutes} min autorisées",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Ajouter du temps bonus :")
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val ref = database.getReference("families").child(familyCode)
                            ref.child("bonusMinutesToday").setValue(bonusMinutes + 15)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("+15m")
                    }
                    Button(
                        onClick = {
                            val ref = database.getReference("families").child(familyCode)
                            ref.child("bonusMinutesToday").setValue(bonusMinutes + 30)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("+30m")
                    }
                }
            }
        }

        // Verrouillage d'urgence
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🛑 Action d'urgence", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val ref = database.getReference("families").child(familyCode)
                            ref.child("dailyLimitMinutes").setValue(0)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("BLOQUER")
                    }
                    Button(
                        onClick = {
                            val ref = database.getReference("families").child(familyCode)
                            ref.child("dailyLimitMinutes").setValue(60)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("DÉBLOQUER")
                    }
                }
            }
        }

        // Vérification de mise à jour
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
                        Toast.makeText(context, "Vous utilisez déjà la dernière version de l'application !", Toast.LENGTH_LONG).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Default.Cloud, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isCheckingUpdate) "Vérification en cours..." else "Vérifier les Mises à Jour (Parent)")
        }
    }

    // Pop-up de mise à jour si disponible
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
