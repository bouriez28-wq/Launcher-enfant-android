package com.example.launchertlphoneenfant.parent.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.launchertlphoneenfant.parent.model.RewardClaimHistory
import com.example.launchertlphoneenfant.parent.model.RoutineItem
import com.example.launchertlphoneenfant.parent.model.StickerMessage
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

@Composable
fun ParentHomeScreen(
    familyCode: String
) {
    val context = LocalContext.current
    var totalStars by remember { mutableIntStateOf(0) }
    var childName by remember { mutableStateOf("Mon Champion") }
    var pendingTasks by remember { mutableStateOf<List<RoutineItem>>(emptyList()) }
    var pendingRewards by remember { mutableStateOf<List<RewardClaimHistory>>(emptyList()) }
    var allRoutines by remember { mutableStateOf<List<RoutineItem>>(emptyList()) }

    var showResetStarsDialog by remember { mutableStateOf(false) }
    var sentStickerMsg by remember { mutableStateOf<String?>(null) }

    val database = remember { FirebaseDatabase.getInstance() }

    // Synchronisation en direct avec Firebase
    LaunchedEffect(familyCode) {
        if (familyCode.isNotBlank()) {
            val familyRef = database.getReference("families").child(familyCode)

            familyRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        totalStars = (snapshot.child("totalStars").value as? Long)?.toInt() ?: 0
                        childName = (snapshot.child("childName").value as? String) ?: "Mon Champion"
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            familyRef.child("routines").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<RoutineItem>()
                    val pending = mutableListOf<RoutineItem>()
                    for (child in snapshot.children) {
                        val item = RoutineItem(
                            id = child.child("id").getValue(String::class.java) ?: "",
                            title = child.child("title").getValue(String::class.java) ?: "",
                            description = child.child("description").getValue(String::class.java) ?: "",
                            icon = child.child("icon").getValue(String::class.java) ?: "⭐",
                            starsReward = (child.child("starsReward").value as? Long)?.toInt() ?: 1,
                            isCompleted = child.child("isCompleted").getValue(Boolean::class.java) ?: false,
                            isPendingValidation = child.child("isPendingValidation").getValue(Boolean::class.java) ?: false
                        )
                        list.add(item)
                        if (item.isPendingValidation) {
                            pending.add(item)
                        }
                    }
                    allRoutines = list
                    pendingTasks = pending
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            familyRef.child("rewardHistory").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<RewardClaimHistory>()
                    for (child in snapshot.children) {
                        val history = RewardClaimHistory(
                            id = child.child("id").getValue(String::class.java) ?: "",
                            rewardTitle = child.child("rewardTitle").getValue(String::class.java) ?: "",
                            rewardIcon = child.child("rewardIcon").getValue(String::class.java) ?: "🎁",
                            starCost = (child.child("starCost").value as? Long)?.toInt() ?: 0,
                            timestamp = child.child("timestamp").getValue(Long::class.java) ?: System.currentTimeMillis(),
                            isPendingApproval = child.child("isPendingApproval").getValue(Boolean::class.java) ?: true
                        )
                        if (history.isPendingApproval) {
                            list.add(history)
                        }
                    }
                    pendingRewards = list
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    val completedToday = allRoutines.count { it.isCompleted }
    val totalToday = allRoutines.size
    val progress = if (totalToday > 0) completedToday.toFloat() / totalToday else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Solde d'étoiles & boutons de gestion
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "⭐ Solde de $childName", fontWeight = FontWeight.Bold, fontSize = 20.sp)
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
                            Text(text = "Points de fidélité et efforts", fontSize = 12.sp, color = Color.Gray)
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
                        Text("+1 ⭐")
                    }
                    Button(
                        onClick = {
                            val ref = database.getReference("families").child(familyCode)
                            ref.child("totalStars").setValue((totalStars - 1).coerceAtLeast(0))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                    ) {
                        Text("-1 ⭐")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { showResetStarsDialog = true }) {
                    Text("🔄 Réinitialiser le solde à 0 ⭐", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        }

        // 2. Statistiques de progression
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "📊 Statistiques de progression", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Tâches accomplies aujourd'hui : $completedToday / $totalToday", fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                )
            }
        }

        // 3. Tâches en attente de validation
        if (pendingTasks.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "⏳ Tâches en attente de validation (${pendingTasks.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                    pendingTasks.forEach { task ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(8.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(task.icon, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(task.title, fontWeight = FontWeight.Bold)
                                    Text("+${task.starsReward} étoile(s) à valider", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = {
                                        val familyRef = database.getReference("families").child(familyCode)
                                        familyRef.child("totalStars").setValue(totalStars + task.starsReward)
                                        val updated = allRoutines.map { if (it.id == task.id) it.copy(isPendingValidation = false, isCompleted = true) else it }
                                        familyRef.child("routines").setValue(updated)
                                        Toast.makeText(context, "Tâche validée ! +${task.starsReward} ⭐", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                ) {
                                    Text("Valider")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Récompenses en attente de validation
        if (pendingRewards.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "🎁 Récompenses demandées (${pendingRewards.size})", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    pendingRewards.forEach { claim ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(8.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(claim.rewardIcon, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(claim.rewardTitle, fontWeight = FontWeight.Bold)
                                    Text("Coût : ${claim.starCost} ⭐", color = Color.Gray, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = {
                                        val familyRef = database.getReference("families").child(familyCode)
                                        familyRef.child("rewardHistory").child(claim.id).child("isPendingApproval").setValue(false)
                                        Toast.makeText(context, "Cadeau approuvé !", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Text("Approuver")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Envoyer un sticker "Bon Point"
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "🌟 Envoyer un sticker \"Bon Point\"", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = "Un clic envoie un sticker festif instantanément sur l'écran de l'enfant !", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))

                val stickerList = listOf(
                    Triple("🌟", "Super travail !", "Tu as assuré aujourd'hui, continue comme ça !"),
                    Triple("🎉", "Bravo champion !", "Félicitations pour tes efforts !"),
                    Triple("🏆", "Le meilleur !", "Tu es le champion de la maison !"),
                    Triple("🚀", "En route vers la victoire !", "Rien ne t'arrête !")
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.height(180.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(stickerList) { (icon, title, msg) ->
                        OutlinedCard(
                            onClick = {
                                val stickerId = database.getReference("families").child(familyCode).child("stickers").push().key ?: java.util.UUID.randomUUID().toString()
                                val sticker = StickerMessage(id = stickerId, icon = icon, title = title, message = msg)
                                database.getReference("families").child(familyCode).child("stickers").child(stickerId).setValue(sticker)
                                sentStickerMsg = "Sticker \"$title\" envoyé à $childName ! 🎉"
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(icon, fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogue confirmation réinitialisation étoiles
    if (showResetStarsDialog) {
        AlertDialog(
            onDismissRequest = { showResetStarsDialog = false },
            title = { Text("Réinitialiser le solde à 0 ⭐ ?") },
            text = { Text("Voulez-vous vraiment remettre les étoiles de $childName à 0 ?") },
            confirmButton = {
                Button(
                    onClick = {
                        database.getReference("families").child(familyCode).child("totalStars").setValue(0)
                        showResetStarsDialog = false
                        Toast.makeText(context, "Solde réinitialisé à 0", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Oui, réinitialiser")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetStarsDialog = false }) { Text("Annuler") }
            }
        )
    }

    // Dialogue sticker envoyé
    sentStickerMsg?.let { msg ->
        AlertDialog(
            onDismissRequest = { sentStickerMsg = null },
            title = { Text("Sticker envoyé 💌", fontWeight = FontWeight.Bold) },
            text = { Text(msg) },
            confirmButton = {
                Button(onClick = { sentStickerMsg = null }) { Text("Super !") }
            }
        )
    }
}
