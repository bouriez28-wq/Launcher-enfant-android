package com.example.launchertlphoneenfant.parent.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.launchertlphoneenfant.parent.model.RewardItem
import com.example.launchertlphoneenfant.parent.model.RoutineItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID

@Composable
fun ParentRoutinesScreen(familyCode: String) {
    var allRoutines by remember { mutableStateOf<List<RoutineItem>>(emptyList()) }
    var allRewards by remember { mutableStateOf<List<RewardItem>>(emptyList()) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Routines, 1 = Magasin

    val database = remember { FirebaseDatabase.getInstance() }

    // Charger les données en direct
    LaunchedEffect(familyCode) {
        if (familyCode.isNotBlank()) {
            val familyRef = database.getReference("families").child(familyCode)

            familyRef.child("routines").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<RoutineItem>()
                    for (child in snapshot.children) {
                        list.add(
                            RoutineItem(
                                id = child.child("id").getValue(String::class.java) ?: "",
                                title = child.child("title").getValue(String::class.java) ?: "",
                                description = child.child("description").getValue(String::class.java) ?: "",
                                icon = child.child("icon").getValue(String::class.java) ?: "⭐",
                                starsReward = (child.child("starsReward").value as? Long)?.toInt() ?: 1
                            )
                        )
                    }
                    allRoutines = list
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            familyRef.child("rewards").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<RewardItem>()
                    for (child in snapshot.children) {
                        list.add(
                            RewardItem(
                                id = child.child("id").getValue(String::class.java) ?: "",
                                title = child.child("title").getValue(String::class.java) ?: "",
                                description = child.child("description").getValue(String::class.java) ?: "",
                                icon = child.child("icon").getValue(String::class.java) ?: "🎁",
                                starCost = (child.child("starCost").value as? Long)?.toInt() ?: 5
                            )
                        )
                    }
                    allRewards = list
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TabRow(selectedTabIndex = selectedTab, modifier = Modifier.fillMaxWidth()) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("📝 Tâches", modifier = Modifier.padding(16.dp))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("🎁 Magasin", modifier = Modifier.padding(16.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            ManageRoutinesList(allRoutines) { newRoutine ->
                val updatedList = allRoutines + newRoutine
                database.getReference("families").child(familyCode).child("routines").setValue(updatedList)
            }
        } else {
            ManageRewardsList(allRewards) { newReward ->
                val updatedList = allRewards + newReward
                database.getReference("families").child(familyCode).child("rewards").setValue(updatedList)
            }
        }
    }
}

@Composable
fun ManageRoutinesList(routines: List<RoutineItem>, onAddRoutine: (RoutineItem) -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Ajouter une Nouvelle Tâche")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(routines) { routine ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(routine.icon, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(routine.title, fontWeight = FontWeight.Bold)
                        Text(routine.description, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("⭐ ${routine.starsReward}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (showAddDialog) {
        AddRoutineDialog(onDismiss = { showAddDialog = false }, onSave = { r -> onAddRoutine(r); showAddDialog = false })
    }
}

@Composable
fun ManageRewardsList(rewards: List<RewardItem>, onAddReward: (RewardItem) -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Ajouter un Nouveau Cadeau")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(rewards) { reward ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(reward.icon, style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(reward.title, fontWeight = FontWeight.Bold)
                        Text(reward.description, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("⭐ ${reward.starCost}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }

    if (showAddDialog) {
        AddRewardDialog(onDismiss = { showAddDialog = false }, onSave = { r -> onAddReward(r); showAddDialog = false })
    }
}

@Composable
fun AddRoutineDialog(onDismiss: () -> Unit, onSave: (RoutineItem) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var reward by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle Tâche") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titre") })
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                OutlinedTextField(value = reward, onValueChange = { reward = it }, label = { Text("Nombre d'étoiles ⭐") })
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(RoutineItem(title = title, description = desc, starsReward = reward.toIntOrNull() ?: 1))
            }) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
fun AddRewardDialog(onDismiss: () -> Unit, onSave: (RewardItem) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var cost by remember { mutableStateOf("5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau Cadeau") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titre (ex: Sortie au parc)") })
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Coût en étoiles ⭐") })
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(RewardItem(title = title, description = desc, starCost = cost.toIntOrNull() ?: 5))
            }) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}
