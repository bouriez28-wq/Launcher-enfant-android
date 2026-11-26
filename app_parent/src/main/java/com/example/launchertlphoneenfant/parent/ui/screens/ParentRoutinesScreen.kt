package com.example.launchertlphoneenfant.parent.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.launchertlphoneenfant.parent.model.RewardItem
import com.example.launchertlphoneenfant.parent.model.RoutineFrequency
import com.example.launchertlphoneenfant.parent.model.RoutineItem
import com.example.launchertlphoneenfant.parent.model.Season
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

                            val seasonStr = child.child("season").getValue(String::class.java) ?: ""
                            val season = if (seasonStr.isNotBlank()) {
                                try { Season.valueOf(seasonStr) } catch (e: Exception) { null }
                            } else null

                            list.add(
                                RoutineItem(
                                    id = child.child("id").getValue(String::class.java) ?: "",
                                    title = child.child("title").getValue(String::class.java) ?: "",
                                    description = child.child("description").getValue(String::class.java) ?: "",
                                    icon = child.child("icon").getValue(String::class.java) ?: "⭐",
                                    starsReward = (child.child("starsReward").value as? Long)?.toInt() ?: 1,
                                    frequency = freq,
                                    timeOfDay = time,
                                    season = season
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
                },
                onUpdateRoutineTime = { routineId, newTime ->
                    val updatedList = allRoutines.map { if (it.id == routineId) it.copy(timeOfDay = newTime) else it }
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
    onDeleteRoutine: (String) -> Unit,
    onUpdateRoutineTime: (String, TimeOfDay) -> Unit
) {
    var subTab by remember { mutableIntStateOf(0) } // 0 = Tâches, 1 = Quotidiennes, 2 = Hebdomadaires, 3 = Mensuelles, 4 = Saisonnières
    var showAddDialog by remember { mutableStateOf(false) }
    var showLibraryDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = subTab, modifier = Modifier.fillMaxWidth()) {
            Tab(selected = subTab == 0, onClick = { subTab = 0 }) { Text("📋 Tâches", modifier = Modifier.padding(10.dp)) }
            Tab(selected = subTab == 1, onClick = { subTab = 1 }) { Text("🔄 Quotidiennes", modifier = Modifier.padding(10.dp)) }
            Tab(selected = subTab == 2, onClick = { subTab = 2 }) { Text("📅 Hebdo", modifier = Modifier.padding(10.dp)) }
            Tab(selected = subTab == 3, onClick = { subTab = 3 }) { Text("📆 Mensuelles", modifier = Modifier.padding(10.dp)) }
            Tab(selected = subTab == 4, onClick = { subTab = 4 }) { Text("🍂 Saisonnier", modifier = Modifier.padding(10.dp)) }
        }
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { focusManager.clearFocus(); showAddDialog = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Créer")
            }
            OutlinedButton(onClick = { focusManager.clearFocus(); showLibraryDialog = true }, modifier = Modifier.weight(1f)) {
                Icon(Icons.AutoMirrored.Filled.LibraryBooks, contentDescription = null)
                Text("Bibliothèque de tâches")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (subTab == 1) {
                // SÉPARATION QUOTIDIENNE : 4 ESPACES DISTINCTS TOUJOURS VISIBLES AVEC GLISSEMENT VERTICAL
                val dailyList = routines.filter { it.frequency == RoutineFrequency.DAILY }

                val morningList = dailyList.filter { it.timeOfDay == TimeOfDay.MORNING }
                val afternoonList = dailyList.filter { it.timeOfDay == TimeOfDay.AFTERNOON }
                val afterSchoolList = dailyList.filter { it.timeOfDay == TimeOfDay.AFTER_SCHOOL }
                val eveningList = dailyList.filter { it.timeOfDay == TimeOfDay.EVENING }

                // 1. Matin
                item { Text("🌅 Matin", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp)) }
                if (morningList.isEmpty()) {
                    item { Text("Aucune tâche le matin. (Appui long puis glisser)", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)) }
                } else {
                    items(morningList, key = { it.id }) { routine -> RoutineCard(routine, onDeleteRoutine, onUpdateRoutineTime) }
                }

                // 2. Après-midi
                item { Text("☀️ Après-midi", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp)) }
                if (afternoonList.isEmpty()) {
                    item { Text("Aucune tâche l'après-midi. (Appui long puis glisser)", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)) }
                } else {
                    items(afternoonList, key = { it.id }) { routine -> RoutineCard(routine, onDeleteRoutine, onUpdateRoutineTime) }
                }

                // 3. Après école
                item { Text("🎒 Après l'école", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp)) }
                if (afterSchoolList.isEmpty()) {
                    item { Text("Aucune tâche après l'école. (Appui long puis glisser)", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)) }
                } else {
                    items(afterSchoolList, key = { it.id }) { routine -> RoutineCard(routine, onDeleteRoutine, onUpdateRoutineTime) }
                }

                // 4. Soir
                item { Text("🌙 Soir", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp, modifier = Modifier.padding(top = 8.dp)) }
                if (eveningList.isEmpty()) {
                    item { Text("Aucune tâche le soir. (Appui long puis glisser)", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)) }
                } else {
                    items(eveningList, key = { it.id }) { routine -> RoutineCard(routine, onDeleteRoutine, onUpdateRoutineTime) }
                }
            } else {
                val filteredRoutines = when (subTab) {
                    0 -> routines // Toutes les tâches
                    2 -> routines.filter { it.frequency == RoutineFrequency.WEEKLY }
                    3 -> routines.filter { it.frequency == RoutineFrequency.MONTHLY }
                    4 -> routines.filter { it.frequency == RoutineFrequency.SEASONAL }
                    else -> routines
                }
                items(filteredRoutines, key = { it.id }) { routine ->
                    RoutineCard(routine, onDeleteRoutine, onUpdateRoutineTime)
                }
            }
        }
    }

    if (showAddDialog) {
        val defaultFreq = when(subTab) {
            1 -> RoutineFrequency.DAILY
            2 -> RoutineFrequency.WEEKLY
            3 -> RoutineFrequency.MONTHLY
            4 -> RoutineFrequency.SEASONAL
            else -> RoutineFrequency.DAILY
        }
        AddRoutineDialog(defaultFrequency = defaultFreq, onDismiss = { showAddDialog = false }, onSave = { r -> onAddRoutine(r); showAddDialog = false })
    }

    if (showLibraryDialog) {
        RoutineLibraryDialog(
            onDismiss = { showLibraryDialog = false },
            onAddRoutineFromLibrary = { routine ->
                onAddRoutine(routine)
                showLibraryDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RoutineCard(
    routine: RoutineItem,
    onDeleteRoutine: (String) -> Unit,
    onUpdateRoutineTime: (String, TimeOfDay) -> Unit
) {
    val categories = listOf(TimeOfDay.MORNING, TimeOfDay.AFTERNOON, TimeOfDay.AFTER_SCHOOL, TimeOfDay.EVENING)
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val elevation = if (isDragging) 10.dp else 2.dp
    val scale = if (isDragging) 1.03f else 1.0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = offsetY
            }
            .pointerInput(routine.id, routine.timeOfDay) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        isDragging = true
                        offsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetY += dragAmount.y
                    },
                    onDragEnd = {
                        isDragging = false
                        val delta = offsetY
                        offsetY = 0f

                        val currentIndex = categories.indexOf(routine.timeOfDay)
                        if (delta < -70f && currentIndex > 0) {
                            // Glissé vers le haut -> catégorie précédente
                            onUpdateRoutineTime(routine.id, categories[currentIndex - 1])
                        } else if (delta > 70f && currentIndex < categories.size - 1) {
                            // Glissé vers le bas -> catégorie suivante
                            onUpdateRoutineTime(routine.id, categories[currentIndex + 1])
                        }
                    },
                    onDragCancel = {
                        offsetY = 0f
                        isDragging = false
                    }
                )
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(routine.icon, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(routine.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                if (routine.description.isNotBlank()) {
                    Text(routine.description, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
                Text(if (isDragging) "⇄ Déplacement en cours..." else "✋ Appui long puis glisser haut/bas", fontSize = 10.sp, color = if (isDragging) MaterialTheme.colorScheme.primary else Color.Gray)
            }
            Text("⭐ ${routine.starsReward}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { onDeleteRoutine(routine.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun ManageRewardsList(
    rewards: List<RewardItem>,
    onAddReward: (RewardItem) -> Unit,
    onDeleteReward: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Button(onClick = { focusManager.clearFocus(); showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
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
    var season by remember { mutableStateOf(Season.SPRING) }
    val focusManager = LocalFocusManager.current

    val iconLibrary = listOf("🛏️", "🪥", "🥣", "📚", "🧸", "🚿", "🎒", "🧹", "🍏", "🥛", "👕", "👟", "⚽", "🎨", "🎵", "🚲", "⭐", "🧼", "🍎", "🧦")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nouvelle Tâche") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text("Choisir une icône :", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    iconLibrary.forEach { emoji ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (icon == emoji) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            onClick = { icon = emoji },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre de la tâche") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (optionnelle)") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )

                Text("Fréquence :", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RoutineFrequency.entries.forEach { freq ->
                        FilterChip(
                            selected = frequency == freq,
                            onClick = { frequency = freq },
                            label = { Text(freq.displayName, fontSize = 11.sp) }
                        )
                    }
                }

                if (frequency == RoutineFrequency.SEASONAL) {
                    Text("Saison :", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Season.entries.forEach { s ->
                            FilterChip(
                                selected = season == s,
                                onClick = { season = s },
                                label = { Text(s.displayName, fontSize = 11.sp) }
                            )
                        }
                    }
                } else {
                    Text("Moment de la journée :", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TimeOfDay.entries.forEach { time ->
                            FilterChip(
                                selected = timeOfDay == time,
                                onClick = { timeOfDay = time },
                                label = { Text(time.displayName, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Text("Étoiles à gagner ⭐ :", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                OutlinedTextField(
                    value = reward,
                    onValueChange = { reward = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                focusManager.clearFocus()
                if (title.isNotBlank()) {
                    onSave(RoutineItem(
                        title = title.trim(),
                        description = desc.trim(),
                        icon = icon,
                        starsReward = reward.toIntOrNull() ?: 1,
                        frequency = frequency,
                        timeOfDay = timeOfDay,
                        season = if (frequency == RoutineFrequency.SEASONAL) season else null
                    ))
                }
            }) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = { focusManager.clearFocus(); onDismiss() }) { Text("Annuler") } }
    )
}

@Composable
fun RoutineLibraryDialog(onDismiss: () -> Unit, onAddRoutineFromLibrary: (RoutineItem) -> Unit) {
    val catalog = getLibraryCatalog()
    var selectedCategory by remember { mutableStateOf("Toutes") }
    var selectedSeasonSubCategory by remember { mutableStateOf("Toutes") }
    val categories = listOf("Toutes", "🍂 Saisons", "🏠 Autonomie", "🧺 Maison", "📚 École")
    val seasons = listOf("Toutes", "🌸 Printemps", "☀️ Été", "🍂 Automne", "❄️ Hiver")
    val focusManager = LocalFocusManager.current

    val filtered = catalog.filter { item ->
        val matchesCategory = when (selectedCategory) {
            "🍂 Saisons" -> item.frequency == RoutineFrequency.SEASONAL
            "🏠 Autonomie" -> item.timeOfDay == TimeOfDay.MORNING || item.timeOfDay == TimeOfDay.EVENING || item.title.contains("lit", true) || item.title.contains("dents", true) || item.title.contains("déjeuner", true) || item.title.contains("habiller", true) || item.title.contains("coiffer", true) || item.title.contains("douche", true) || item.title.contains("mains", true) || item.title.contains("eau", true)
            "🧺 Maison" -> item.title.contains("chambre", true) || item.title.contains("table", true) || item.title.contains("balai", true) || item.title.contains("linge", true) || item.title.contains("poubelle", true) || item.title.contains("animal", true) || item.title.contains("plantes", true) || item.title.contains("manteau", true) || item.title.contains("courses", true) || item.title.contains("jouets", true)
            "📚 École" -> item.title.contains("devoirs", true) || item.title.contains("cartable", true) || item.title.contains("école", true) || item.title.contains("livre", true) || item.title.contains("leçon", true) || item.title.contains("bureau", true)
            else -> true
        }

        val matchesSeason = if (selectedCategory == "🍂 Saisons" && selectedSeasonSubCategory != "Toutes") {
            when (selectedSeasonSubCategory) {
                "🌸 Printemps" -> item.season == Season.SPRING
                "☀️ Été" -> item.season == Season.SUMMER
                "🍂 Automne" -> item.season == Season.AUTUMN
                "❄️ Hiver" -> item.season == Season.WINTER
                else -> true
            }
        } else true

        matchesCategory && matchesSeason
    }

    AlertDialog(
        onDismissRequest = { focusManager.clearFocus(); onDismiss() },
        title = { Text("📚 Bibliothèque de Tâches", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().height(450.dp)) {
                ScrollableTabRow(selectedTabIndex = categories.indexOf(selectedCategory)) {
                    categories.forEach { cat ->
                        Tab(
                            selected = selectedCategory == cat,
                            onClick = {
                                selectedCategory = cat
                                selectedSeasonSubCategory = "Toutes"
                            },
                            text = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }

                if (selectedCategory == "🍂 Saisons") {
                    Spacer(modifier = Modifier.height(4.dp))
                    ScrollableTabRow(selectedTabIndex = seasons.indexOf(selectedSeasonSubCategory)) {
                        seasons.forEach { s ->
                            Tab(
                                selected = selectedSeasonSubCategory == s,
                                onClick = { selectedSeasonSubCategory = s },
                                text = { Text(s, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(filtered) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { focusManager.clearFocus(); onAddRoutineFromLibrary(item) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(item.icon, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    val badge = if (item.frequency == RoutineFrequency.SEASONAL && item.season != null) "${item.season.displayName} 🍂" else item.frequency.displayName
                                    Text("$badge • ⭐ ${item.starsReward} étoile(s)", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                                }
                                Button(onClick = { focusManager.clearFocus(); onAddRoutineFromLibrary(item) }) {
                                    Text("Ajouter", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { focusManager.clearFocus(); onDismiss() }) { Text("Fermer") }
        }
    )
}

@Composable
fun AddRewardDialog(onDismiss: () -> Unit, onSave: (RewardItem) -> Unit) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("🎁") }
    var cost by remember { mutableStateOf("5") }
    val focusManager = LocalFocusManager.current

    val giftIcons = listOf("🎁", "🎮", "🍕", "🛝", "🌙", "🚲", "🍦", "🎬", "⚽", "🧸", "🎉", "🍬")

    AlertDialog(
        onDismissRequest = { focusManager.clearFocus(); onDismiss() },
        title = { Text("Nouveau Cadeau au Magasin") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text("Choisir une icône :", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    giftIcons.forEach { emoji ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (icon == emoji) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            onClick = { icon = emoji },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre du Cadeau") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                )
                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it },
                    label = { Text("Coût en étoiles ⭐") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                focusManager.clearFocus()
                if (title.isNotBlank()) {
                    onSave(RewardItem(title = title.trim(), description = desc.trim(), icon = icon, starCost = cost.toIntOrNull() ?: 5))
                }
            }) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = { focusManager.clearFocus(); onDismiss() }) { Text("Annuler") } }
    )
}

fun getDefaultRoutines(): List<RoutineItem> {
    return listOf(
        RoutineItem(id = "def_r_1", title = "Faire son lit", icon = "🛏️", timeOfDay = TimeOfDay.MORNING, frequency = RoutineFrequency.DAILY, starsReward = 1),
        RoutineItem(id = "def_r_2", title = "Brosser ses dents", icon = "🪥", timeOfDay = TimeOfDay.MORNING, frequency = RoutineFrequency.DAILY, starsReward = 1),
        RoutineItem(id = "def_r_3", title = "Prendre son petit-déjeuner", icon = "🥣", timeOfDay = TimeOfDay.MORNING, frequency = RoutineFrequency.DAILY, starsReward = 1),
        RoutineItem(id = "def_r_4", title = "Faire ses devoirs", icon = "📚", timeOfDay = TimeOfDay.AFTER_SCHOOL, frequency = RoutineFrequency.DAILY, starsReward = 2),
        RoutineItem(id = "def_r_5", title = "Ranger sa chambre", icon = "🧸", timeOfDay = TimeOfDay.AFTER_SCHOOL, frequency = RoutineFrequency.DAILY, starsReward = 2),
        RoutineItem(id = "def_r_6", title = "Prendre sa douche", icon = "🚿", timeOfDay = TimeOfDay.EVENING, frequency = RoutineFrequency.DAILY, starsReward = 1),
        RoutineItem(id = "def_r_7", title = "Brosser ses dents (Soir)", icon = "🪥", timeOfDay = TimeOfDay.EVENING, frequency = RoutineFrequency.DAILY, starsReward = 1)
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

fun getLibraryCatalog(): List<RoutineItem> {
    return listOf(
        // Printemps 🌸
        RoutineItem(title = "Planter des fleurs ou des graines", icon = "🌸", starsReward = 2, frequency = RoutineFrequency.SEASONAL, season = Season.SPRING),
        RoutineItem(title = "Nettoyer son vélo pour les beaux jours", icon = "🚲", starsReward = 2, frequency = RoutineFrequency.SEASONAL, season = Season.SPRING),
        RoutineItem(title = "Grand ménage de printemps de la chambre", icon = "🧺", starsReward = 3, frequency = RoutineFrequency.SEASONAL, season = Season.SPRING),
        RoutineItem(title = "Ramasser des pâquerettes", icon = "🌼", starsReward = 1, frequency = RoutineFrequency.SEASONAL, season = Season.SPRING),

        // Été ☀️
        RoutineItem(title = "Arroser le jardin et les plantes", icon = "☀️", starsReward = 2, frequency = RoutineFrequency.SEASONAL, season = Season.SUMMER),
        RoutineItem(title = "Mettre de la crème solaire", icon = "🧴", starsReward = 1, frequency = RoutineFrequency.SEASONAL, season = Season.SUMMER),
        RoutineItem(title = "Mettre sa casquette / chapeau", icon = "🧢", starsReward = 1, frequency = RoutineFrequency.SEASONAL, season = Season.SUMMER),
        RoutineItem(title = "Rincer et ranger les jeux de plage", icon = "🏖️", starsReward = 1, frequency = RoutineFrequency.SEASONAL, season = Season.SUMMER),

        // Automne 🍂
        RoutineItem(title = "Ramasser les feuilles mortes", icon = "🍂", starsReward = 3, frequency = RoutineFrequency.SEASONAL, season = Season.AUTUMN),
        RoutineItem(title = "Décorer la maison pour l'automne", icon = "🎃", starsReward = 2, frequency = RoutineFrequency.SEASONAL, season = Season.AUTUMN),
        RoutineItem(title = "Ranger ses bottes de pluie", icon = "🌧️", starsReward = 1, frequency = RoutineFrequency.SEASONAL, season = Season.AUTUMN),
        RoutineItem(title = "Ramasser des châtaignes et marrons", icon = "🌰", starsReward = 2, frequency = RoutineFrequency.SEASONAL, season = Season.AUTUMN),

        // Hiver ❄️
        RoutineItem(title = "Déneiger l'allée / le devant", icon = "❄️", starsReward = 3, frequency = RoutineFrequency.SEASONAL, season = Season.WINTER),
        RoutineItem(title = "Nourrir les oiseaux du jardin", icon = "🐦", starsReward = 1, frequency = RoutineFrequency.SEASONAL, season = Season.WINTER),
        RoutineItem(title = "Mettre bonnet, écharpe et gants", icon = "🧣", starsReward = 1, frequency = RoutineFrequency.SEASONAL, season = Season.WINTER),
        RoutineItem(title = "Préparer un bon chocolat chaud", icon = "☕", starsReward = 1, frequency = RoutineFrequency.SEASONAL, season = Season.WINTER),

        // Autonomie 🏠
        RoutineItem(title = "Faire son lit", icon = "🛏️", starsReward = 1, timeOfDay = TimeOfDay.MORNING),
        RoutineItem(title = "Se brosser les dents (Matin)", icon = "🪥", starsReward = 1, timeOfDay = TimeOfDay.MORNING),
        RoutineItem(title = "Prendre son petit-déjeuner", icon = "🥣", starsReward = 1, timeOfDay = TimeOfDay.MORNING),
        RoutineItem(title = "S'habiller tout seul", icon = "👕", starsReward = 1, timeOfDay = TimeOfDay.MORNING),
        RoutineItem(title = "Se coiffer / brosser les cheveux", icon = "💇", starsReward = 1, timeOfDay = TimeOfDay.MORNING),
        RoutineItem(title = "Boire un grand verre d'eau", icon = "🥛", starsReward = 1, timeOfDay = TimeOfDay.MORNING),
        RoutineItem(title = "Prendre sa douche / son bain", icon = "🚿", starsReward = 1, timeOfDay = TimeOfDay.EVENING),
        RoutineItem(title = "Se brosser les dents (Soir)", icon = "🪥", starsReward = 1, timeOfDay = TimeOfDay.EVENING),
        RoutineItem(title = "Se laver les mains avant de manger", icon = "🧼", starsReward = 1, timeOfDay = TimeOfDay.EVENING),

        // Maison 🧺
        RoutineItem(title = "Ranger sa chambre et ses jouets", icon = "🧸", starsReward = 2, timeOfDay = TimeOfDay.AFTER_SCHOOL),
        RoutineItem(title = "Débarrasser la table après le repas", icon = "🍽️", starsReward = 1, timeOfDay = TimeOfDay.EVENING),
        RoutineItem(title = "Mettre la table", icon = "🍴", starsReward = 1, timeOfDay = TimeOfDay.EVENING),
        RoutineItem(title = "Passer le balai ou l'aspirateur", icon = "🧹", starsReward = 2, timeOfDay = TimeOfDay.AFTER_SCHOOL),
        RoutineItem(title = "Mettre le linge sale dans le panier", icon = "🧺", starsReward = 1, timeOfDay = TimeOfDay.EVENING),
        RoutineItem(title = "Nourrir l'animal de compagnie", icon = "🐶", starsReward = 2, timeOfDay = TimeOfDay.AFTERNOON),
        RoutineItem(title = "Aider à ranger les courses", icon = "🛒", starsReward = 2, timeOfDay = TimeOfDay.AFTER_SCHOOL),
        RoutineItem(title = "Ranger ses chaussures et son manteau", icon = "🧥", starsReward = 1, timeOfDay = TimeOfDay.AFTER_SCHOOL),

        // École 📚
        RoutineItem(title = "Faire ses devoirs calmement", icon = "📚", starsReward = 2, timeOfDay = TimeOfDay.AFTER_SCHOOL),
        RoutineItem(title = "Préparer son cartable", icon = "🎒", starsReward = 1, timeOfDay = TimeOfDay.EVENING),
        RoutineItem(title = "Lire un livre pendant 15 minutes", icon = "📖", starsReward = 1, timeOfDay = TimeOfDay.EVENING),
        RoutineItem(title = "Apprendre sa poésie ou ses leçons", icon = "🧠", starsReward = 2, timeOfDay = TimeOfDay.AFTER_SCHOOL),
        RoutineItem(title = "Ranger son bureau d'école", icon = "✏️", starsReward = 1, timeOfDay = TimeOfDay.AFTER_SCHOOL)
    )
}
