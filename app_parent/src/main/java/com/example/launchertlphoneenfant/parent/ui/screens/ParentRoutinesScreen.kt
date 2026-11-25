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

@Composable
fun ParentRoutinesScreen(familyCode: String) {
    var allRoutines by remember { mutableStateOf<List<RoutineItem>>(emptyList()) }
    var allRewards by remember { mutableStateOf<List<RewardItem>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Routines, 1 = Magasin

    val database = remember { FirebaseDatabase.getInstance() }

    // Charger les données en direct et initialiser les valeurs par défaut si vide
    LaunchedEffect(familyCode) {
        if (familyCode.isNotBlank()) {
            val familyRef = database.getReference("families").child(familyCode)

            familyRef.child("routines").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<RoutineItem>()
                    if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                        val defaults = getDefaultRoutines()
                        familyRef.child("routines").setValue(defaults)
                    } else {
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
                    }
                    allRoutines = list
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            familyRef.child("rewards").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<RewardItem>()
                    if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                        val defaults = getDefaultRewards()
                        familyRef.child("rewards").setValue(defaults)
                    } else {
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
                Text("📝 Tâches & Routines", modifier = Modifier.padding(16.dp))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("🎁 Magasin de Cadeaux", modifier = Modifier.padding(16.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            ManageRoutinesList(
                routines = allRoutines,
                onAddRoutine = { newRoutine ->
                    val updatedList = allRoutines + newRoutine
                    database.getReference("families").child(familyCode).child("routines").setValue(updatedList)
                },
                onDeleteRoutine = { routineId ->
                    val updatedList = allRoutines.filter { it.id != routineId }
                    database.getReference("families").child(familyCode).child("routines").setValue(updatedList)
                }
            )
        } else {
            ManageRewardsList(
                rewards = allRewards,
                onAddReward = { newReward ->
                    val updatedList = allRewards + newReward
                    database.getReference("families").child(familyCode).child("rewards").setValue(updatedList)
                },
                onDeleteReward = { rewardId ->
                    val updatedList = allRewards.filter { it.id != rewardId }
                    database.getReference("families").child(familyCode).child("rewards").setValue(updatedList)
                }
            )
        }
    }
}

@Composable
fun ManageRoutinesList(
    routines: List<RoutineItem>,
    onAddRoutine: (RoutineItem) -> Unit,
    onDeleteRoutine: (String) -> Unit
) {
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
                        if (routine.description.isNotBlank()) {
                            Text(routine.description, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text("⭐ ${routine.starsReward}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { onDeleteRoutine(routine.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRoutineDialog(onDismiss = { showAddDialog = false }, onSave = { r -> onAddRoutine(r); showAddDialog = false })
    }
}

@Composable
fun ManageRewardsList(
    rewards: List<RewardItem>,
    onAddReward: (RewardItem) -> Unit,
    onDeleteReward: (String) -> Unit
) {
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
                        if (reward.description.isNotBlank()) {
                            Text(reward.description, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text("⭐ ${reward.starCost}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { onDeleteReward(reward.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                    }
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
    var icon by remember { mutableStateOf("⭐") }
    var reward by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle Tâche") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = icon, onValueChange = { icon = it }, label = { Text("Icône (Emoji ex: 🛏️)") }, singleLine = true)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titre") }, singleLine = true)
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description (optionnelle)") })
                OutlinedTextField(value = reward, onValueChange = { reward = it }, label = { Text("Étoiles à gagner ⭐") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number), singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank()) {
                    onSave(RoutineItem(title = title, description = desc, icon = icon, starsReward = reward.toIntOrNull() ?: 1))
                }
            }) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

@Composable
fun AddRewardDialog(onDismiss: () -> Unit, onSave: (RewardItem) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("🎁") }
    var cost by remember { mutableStateOf("5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouveau Cadeau") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = icon, onValueChange = { icon = it }, label = { Text("Icône (Emoji ex: 🎮)") }, singleLine = true)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titre du Cadeau") }, singleLine = true)
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Coût en étoiles ⭐") }, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number), singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank()) {
                    onSave(RewardItem(title = title, description = desc, icon = icon, starCost = cost.toIntOrNull() ?: 5))
                }
            }) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
}

fun getDefaultRoutines(): List<RoutineItem> {
    return listOf(
        RoutineItem(id = "def_r_1", title = "Faire son lit", icon = "🛏️", starsReward = 1),
        RoutineItem(id = "def_r_2", title = "Brosser ses dents", icon = "🪥", starsReward = 1),
        RoutineItem(id = "def_r_3", title = "Prendre son petit-déjeuner", icon = "🥣", starsReward = 1),
        RoutineItem(id = "def_r_4", title = "Faire ses devoirs", icon = "📚", starsReward = 2),
        RoutineItem(id = "def_r_5", title = "Ranger ses jouets", icon = "🧸", starsReward = 2),
        RoutineItem(id = "def_r_6", title = "Prendre sa douche", icon = "🚿", starsReward = 1),
        RoutineItem(id = "def_r_7", title = "Lire une histoire", icon = "📖", starsReward = 1)
    )
}

fun getDefaultRewards(): List<RewardItem> {
    return listOf(
        RewardItem(id = "def_w_1", title = "30 min de jeux / écran", icon = "🎮", starCost = 5, description = "Moment détente console ou tablette"),
        RewardItem(id = "def_w_2", title = "Choisir le repas du soir", icon = "🍕", starCost = 8, description = "Proposer son plat préféré pour le dîner"),
        RewardItem(id = "def_w_3", title = "Sortie au parc", icon = "🛝", starCost = 10, description = "Aller jouer au toboggan"),
        RewardItem(id = "def_w_4", title = "Coucher +15 minutes", icon = "🌙", starCost = 6, description = "Rester éveillé un peu plus tard le soir")
    )
}
