package com.example.launchertlphoneenfant.parent.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.launchertlphoneenfant.parent.model.RewardClaimHistory
import com.example.launchertlphoneenfant.parent.model.RoutineItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

@Composable
fun ParentValidationScreen(familyCode: String) {
    var allRoutines by remember { mutableStateOf<List<RoutineItem>>(emptyList()) }
    var allHistory by remember { mutableStateOf<List<RewardClaimHistory>>(emptyList()) }
    var totalStars by remember { mutableIntStateOf(0) }

    val database = remember { FirebaseDatabase.getInstance() }

    // Charger les données en direct de Firebase
    LaunchedEffect(familyCode) {
        if (familyCode.isNotBlank()) {
            val familyRef = database.getReference("families").child(familyCode)

            // Écoute des Étoiles
            familyRef.child("totalStars").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    totalStars = (snapshot.value as? Long)?.toInt() ?: 0
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            // Écoute des Routines
            familyRef.child("routines").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<RoutineItem>()
                    for (child in snapshot.children) {
                        val routine = RoutineItem(
                            id = child.child("id").getValue(String::class.java) ?: "",
                            title = child.child("title").getValue(String::class.java) ?: "",
                            description = child.child("description").getValue(String::class.java) ?: "",
                            icon = child.child("icon").getValue(String::class.java) ?: "⭐",
                            starsReward = (child.child("starsReward").value as? Long)?.toInt() ?: 1,
                            isCompleted = child.child("isCompleted").getValue(Boolean::class.java) ?: false,
                            isPendingValidation = child.child("isPendingValidation").getValue(Boolean::class.java) ?: false
                        )
                        list.add(routine)
                    }
                    allRoutines = list
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            // Écoute de l'Historique des Récompenses
            familyRef.child("history").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<RewardClaimHistory>()
                    for (child in snapshot.children) {
                        val claim = RewardClaimHistory(
                            id = child.child("id").getValue(String::class.java) ?: "",
                            rewardTitle = child.child("rewardTitle").getValue(String::class.java) ?: "",
                            rewardIcon = child.child("rewardIcon").getValue(String::class.java) ?: "🎁",
                            starCost = (child.child("starCost").value as? Long)?.toInt() ?: 0,
                            timestamp = (child.child("timestamp").value as? Long) ?: 0L,
                            isPendingApproval = child.child("isPendingApproval").getValue(Boolean::class.java) ?: false
                        )
                        list.add(claim)
                    }
                    allHistory = list
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    val pendingRoutines = allRoutines.filter { it.isPendingValidation }
    val pendingRewards = allHistory.filter { it.isPendingApproval }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Solde de l'enfant : ⭐ $totalStars étoiles", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (pendingRoutines.isEmpty() && pendingRewards.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = "Aucune validation en attente ! 🎉",
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        if (pendingRoutines.isNotEmpty()) {
            item {
                Text("Tâches à vérifier", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            items(pendingRoutines) { routine ->
                ValidationCard(
                    icon = routine.icon,
                    title = routine.title,
                    subtitle = "Gain : ⭐ ${routine.starsReward}",
                    onApprove = {
                        val updatedList = allRoutines.map {
                            if (it.id == routine.id) it.copy(isPendingValidation = false, isCompleted = true) else it
                        }
                        val ref = database.getReference("families").child(familyCode)
                        ref.child("routines").setValue(updatedList)
                        ref.child("totalStars").setValue(totalStars + routine.starsReward)
                    },
                    onReject = {
                        val updatedList = allRoutines.map {
                            if (it.id == routine.id) it.copy(isPendingValidation = false, isCompleted = false) else it
                        }
                        database.getReference("families").child(familyCode).child("routines").setValue(updatedList)
                    }
                )
            }
        }

        if (pendingRewards.isNotEmpty()) {
            item {
                Text("Récompenses demandées", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            }
            items(pendingRewards) { claim ->
                ValidationCard(
                    icon = claim.rewardIcon,
                    title = claim.rewardTitle,
                    subtitle = "Coût : ⭐ ${claim.starCost}",
                    onApprove = {
                        val updatedList = allHistory.map {
                            if (it.id == claim.id) it.copy(isPendingApproval = false) else it
                        }
                        database.getReference("families").child(familyCode).child("history").setValue(updatedList)
                    },
                    onReject = {
                        // Rembourser l'enfant et supprimer la demande
                        val updatedList = allHistory.filter { it.id != claim.id }
                        val ref = database.getReference("families").child(familyCode)
                        ref.child("history").setValue(updatedList)
                        ref.child("totalStars").setValue(totalStars + claim.starCost)
                    }
                )
            }
        }
    }
}

@Composable
fun ValidationCard(
    icon: String,
    title: String,
    subtitle: String,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold)
                Text(text = subtitle, color = Color.Gray)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onReject,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Refuser", tint = MaterialTheme.colorScheme.onErrorContainer)
                }
                IconButton(
                    onClick = onApprove,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Approuver", tint = Color.White)
                }
            }
        }
    }
}
