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
import com.example.launchertlphoneenfant.parent.model.RoutineFrequency
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
    var weeklyStars by remember { mutableStateOf(listOf(0, 0, 0, 0, 0, 0, 0)) }

    var showResetStarsDialog by remember { mutableStateOf(false) }
    var showResetStatsDialog by remember { mutableStateOf(false) }
    var sentStickerMsg by remember { mutableStateOf<String?>(null) }

    // Dialog ajustement étoiles (+ / - avec commentaire)
    var showStarDialog by remember { mutableStateOf(false) }
    var starDialogMode by remember { mutableStateOf(true) } // true = +, false = -
    var starInputAmount by remember { mutableStateOf("1") }
    var starInputComment by remember { mutableStateOf("") }

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
                            isPendingValidation = child.child("isPendingValidation").getValue(Boolean::class.java) ?: false,
                            frequency = try { RoutineFrequency.valueOf(child.child("frequency").getValue(String::class.java) ?: "DAILY") } catch (e: Exception) { RoutineFrequency.DAILY }
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

            familyRef.child("weeklyStars").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf(0, 0, 0, 0, 0, 0, 0)
                    for (i in 0..6) {
                        list[i] = (snapshot.child(i.toString()).value as? Long)?.toInt() ?: 0
                    }
                    weeklyStars = list
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Solde d'étoiles & boutons de gestion (+ / - avec commentaire)
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
                            starDialogMode = true
                            starInputAmount = "1"
                            starInputComment = ""
                            showStarDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("+ Ajouter des étoiles")
                    }
                    Button(
                        onClick = {
                            starDialogMode = false
                            starInputAmount = "1"
                            starInputComment = ""
                            showStarDialog = true
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("- Retirer des étoiles")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { showResetStarsDialog = true }) {
                    Text("🔄 Réinitialiser le solde à 0 ⭐", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        }

        // 2. Statistiques de progression (Identiques à Routinercompense avec réinitialisation complète)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "📊 Statistiques de progression", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(12.dp))

                // Progression journalière
                val dailyRoutines = allRoutines.filter { it.frequency == RoutineFrequency.DAILY }
                val dailyCompleted = dailyRoutines.count { it.isCompleted }
                val dailyTotal = dailyRoutines.size
                val dailyPercent = if (dailyTotal > 0) (dailyCompleted * 100 / dailyTotal) else 0
                val dailyFloat = if (dailyTotal > 0) dailyCompleted.toFloat() / dailyTotal else 0f

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Progression journalière :", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "$dailyPercent%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { dailyFloat },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Progression mensuelle
                val monthlyRoutines = allRoutines.filter { it.frequency == RoutineFrequency.MONTHLY }
                val monthlyCompleted = monthlyRoutines.count { it.isCompleted }
                val monthlyTotal = monthlyRoutines.size
                val monthlyPercent = if (monthlyTotal > 0) (monthlyCompleted * 100 / monthlyTotal) else 0
                val monthlyFloat = if (monthlyTotal > 0) monthlyCompleted.toFloat() / monthlyTotal else 0f

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Progression mensuelle :", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "$monthlyPercent%",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { monthlyFloat },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF2E7D32),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Étoiles gagnées cette semaine (Réinitialisé dimanche soir) :", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))

                val daysOfWeekLabels = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    daysOfWeekLabels.forEachIndexed { idx, dayName ->
                        val dayStars = weeklyStars.getOrElse(idx) { 0 }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text = if (dayStars > 0) "$dayStars" else "0",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dayStars > 0) Color(0xFFF57F17) else Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = if (dayStars > 0) Color(0xFFFFB300) else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(((dayStars * 10).coerceIn(12, 70)).dp)
                            ) {}
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = dayName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showResetStatsDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🔄 Réinitialiser les statistiques", fontWeight = FontWeight.Bold)
                }
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

                                        // Mettre à jour les étoiles de la semaine pour le jour actuel
                                        val cal = java.util.Calendar.getInstance()
                                        val calDay = cal.get(java.util.Calendar.DAY_OF_WEEK)
                                        val dayIndex = if (calDay == java.util.Calendar.SUNDAY) 6 else calDay - 2
                                        if (dayIndex in 0..6) {
                                            val currentDayStars = weeklyStars.getOrElse(dayIndex) { 0 }
                                            familyRef.child("weeklyStars").child(dayIndex.toString()).setValue(currentDayStars + task.starsReward)
                                        }

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

    // Dialog ajustement étoiles (+ / - avec commentaire)
    if (showStarDialog) {
        AlertDialog(
            onDismissRequest = { showStarDialog = false },
            title = { Text(if (starDialogMode) "Ajouter des étoiles ⭐" else "Retirer des étoiles ⭐") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = starInputAmount,
                        onValueChange = { starInputAmount = it },
                        label = { Text("Nombre d'étoiles") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = starInputComment,
                        onValueChange = { starInputComment = it },
                        label = { Text("Commentaire / Raison") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = starInputAmount.toIntOrNull() ?: 0
                        if (amount > 0) {
                            val newTotal = if (starDialogMode) totalStars + amount else (totalStars - amount).coerceAtLeast(0)
                            database.getReference("families").child(familyCode).child("totalStars").setValue(newTotal)
                            showStarDialog = false
                            Toast.makeText(context, "Solde mis à jour (${if (starDialogMode) "+$amount" else "-$amount"} ⭐)", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Confirmer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStarDialog = false }) { Text("Annuler") }
            }
        )
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

    // Dialogue confirmation réinitialisation statistiques (avec réinitialisation des étoiles de la semaine et des routines)
    if (showResetStatsDialog) {
        AlertDialog(
            onDismissRequest = { showResetStatsDialog = false },
            title = { Text("Réinitialiser les statistiques ?") },
            text = { Text("Voulez-vous vraiment réinitialiser toutes les statistiques de progression et les étoiles de la semaine à zéro ?") },
            confirmButton = {
                Button(
                    onClick = {
                        val familyRef = database.getReference("families").child(familyCode)
                        val resetRoutines = allRoutines.map { it.copy(isCompleted = false, isPendingValidation = false) }
                        familyRef.child("routines").setValue(resetRoutines)
                        familyRef.child("weeklyStars").setValue(listOf(0, 0, 0, 0, 0, 0, 0))
                        showResetStatsDialog = false
                        Toast.makeText(context, "Statistiques réinitialisées", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Oui, réinitialiser")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetStatsDialog = false }) { Text("Annuler") }
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
