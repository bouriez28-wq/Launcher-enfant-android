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
import androidx.compose.ui.unit.sp
import com.example.launchertlphoneenfant.parent.model.RewardItem
import com.example.launchertlphoneenfant.parent.model.RoutineFrequency
import com.example.launchertlphoneenfant.parent.model.RoutineItem
import com.example.launchertlphoneenfant.parent.model.TimeOfDay
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

@Composable
fun ParentRoutinesScreen(familyCode: String) {
    var allRoutines by remember { mutableStateOf<List<RoutineItem>>(emptyList()) }
    var allRewards by remember { mutableStateOf<List<RewardItem>>(emptyList()) }
    var mainTab by remember { mutableIntStateOf(0) } // 0 = Tâches, 1 = Magasin

    val database = remember { FirebaseDatabase.getInstance() }

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
                            val freqStr = child.child("frequency").getValue(String::class.java) ?: RoutineFrequency.DAILY.name
                            val freq = try { RoutineFrequency.valueOf(freqStr) } catch (e: Exception) { RoutineFrequency.DAILY }

                            val timeStr = child.child("timeOfDay").getValue(String::class.java) ?: TimeOfDay.MORNING.name
                            val time = try { TimeOfDay.valueOf(timeStr) } catch (e: Exception) { TimeOfDay.MORNING }

                            list.add(
                                RoutineItem(
                                    id = child.child("id").getValue(String::class.java) ?: "",
                                    title = child.child("title").getValue(String::class.java) ?: "",
                                    description = child.child("description").getValue(String::class.java) ?: "",
                                    icon = child.child("icon").getValue(String::class.java) ?: "⭐",
                                    starsReward = (child.child("starsReward").value as? Long)?.toInt() ?: 1,
                                    frequency = freq,
                                    timeOfDay = time
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
        TabRow(selectedTabIndex = mainTab, modifier = Modifier.fillMaxWidth()) {
            Tab(selected = mainTab == 0, onClick = { mainTab = 0 }) {
                Text("📝 Tâches & Routines", modifier = Modifier.padding(16.dp))
            }
            Tab(selected = mainTab == 1, onClick = { mainTab = 1 }) {
                Text("🎁 Magasin de Cadeaux", modifier = Modifier.padding(16.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (mainTab == 0) {
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
    var subTab by remember { mutableIntStateOf(0) } // 0 = Quotidiennes, 1 = Hebdomadaires, 2 = Mensuelles
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredRoutines = when (subTab) {
        0 -> routines.filter { it.frequency == RoutineFrequency.DAILY }
        1 -> routines.filter { it.frequency == RoutineFrequency.WEEKLY }
        2 -> routines.filter { it.frequency == RoutineFrequency.MONTHLY }
        else -> routines
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = subTab, modifier = Modifier.fillMaxWidth()) {
            Tab(selected = subTab == 0, onClick = { subTab = 0 }) { Text("🔄 Quotidiennes", modifier = Modifier.padding(12.dp)) }
            Tab(selected = subTab == 1, onClick = { subTab = 1 }) { Text("📅 Hebdomadaires", modifier = Modifier.padding(12.dp)) }
            Tab(selected = subTab == 2, onClick = { subTab = 2 }) { Text("📆 Mensuelles", modifier = Modifier.padding(12.dp)) }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("Ajouter une Tâche (${when(subTab) { 0 -> "Quotidienne"; 1 -> "Hebdomadaire"; else -> "Mensuelle" }})")
        }
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filteredRoutines) { routine ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(routine.icon, style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(routine.title, fontWeight = FontWeight.Bold)
                                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp)) {
                                    Text(routine.timeOfDay.displayName, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
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
    }

    if (showAddDialog) {
        val defaultFreq = when(subTab) {
            0 -> RoutineFrequency.DAILY
            1 -> RoutineFrequency.WEEKLY
            else -> RoutineFrequency.MONTHLY
        }
        AddRoutineDialog(defaultFrequency = defaultFreq, onDismiss = { showAddDialog = false }, onSave = { r -> onAddRoutine(r); showAddDialog = false })
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
                Text("Ajouter un Nouveau Cadeau au Magasin")
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
fun AddRoutineDialog(defaultFrequency: RoutineFrequency, onDismiss: () -> Unit, onSave: (RoutineItem) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("⭐") }
    var reward by remember { mutableStateOf("1") }
    var frequency by remember { mutableStateOf(defaultFrequency) }
    var timeOfDay by remember { mutableStateOf(TimeOfDay.MORNING) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle Tâche (${frequency.displayName})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = icon, onValueChange = { icon = it }, label = { Text("Icône (Emoji ex: 🛏️)") }, singleLine = true)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titre") }, singleLine = true)
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description (optionnelle)") })

                Text("Moment de la journée :", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TimeOfDay.entries.forEach { time ->
                        FilterChip(
                            selected = timeOfDay == time,
                            onClick = { timeOfDay = time },
                            label = { Text(time.displayName, fontSize = 11.sp) }
                        )
                    }
                }

                Text("Étoiles à gagner ⭐ :", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                OutlinedTextField(
                    value = reward,
                    onValueChange = { reward = it },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank()) {
                    onSave(RoutineItem(title = title, description = desc, icon = icon, starsReward = reward.toIntOrNull() ?: 1, frequency = frequency, timeOfDay = timeOfDay))
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
        title = { Text("Nouveau Cadeau au Magasin") },
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
        RoutineItem(id = "def_r_1", title = "Faire son lit", icon = "🛏️", timeOfDay = TimeOfDay.MORNING, frequency = RoutineFrequency.DAILY, starsReward = 1),
        RoutineItem(id = "def_r_2", title = "Brosser ses dents", icon = "🪥", timeOfDay = TimeOfDay.MORNING, frequency = RoutineFrequency.DAILY, starsReward = 1),
        RoutineItem(id = "def_r_3", title = "Prendre son petit-déjeuner", icon = "🥣", timeOfDay = TimeOfDay.MORNING, frequency = RoutineFrequency.DAILY, starsReward = 1),
        RoutineItem(id = "def_r_4", title = "Ranger sa chambre", icon = "🧹", timeOfDay = TimeOfDay.AFTERNOON, frequency = RoutineFrequency.WEEKLY, starsReward = 3),
        RoutineItem(id = "def_r_5", title = "Faire ses devoirs", icon = "📚", timeOfDay = TimeOfDay.AFTERNOON, frequency = RoutineFrequency.DAILY, starsReward = 2),
        RoutineItem(id = "def_r_6", title = "Prendre sa douche", icon = "🚿", timeOfDay = TimeOfDay.EVENING, frequency = RoutineFrequency.DAILY, starsReward = 1),
        RoutineItem(id = "def_r_7", title = "Grand rangement mensuel", icon = "📦", timeOfDay = TimeOfDay.AFTERNOON, frequency = RoutineFrequency.MONTHLY, starsReward = 10)
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
