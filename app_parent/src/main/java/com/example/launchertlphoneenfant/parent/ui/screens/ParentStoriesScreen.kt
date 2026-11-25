package com.example.launchertlphoneenfant.parent.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.launchertlphoneenfant.parent.model.ChildProfile
import com.example.launchertlphoneenfant.parent.model.WeeklyTheme
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

@Composable
fun ParentStoriesScreen(familyCode: String) {
    var childrenProfiles by remember { mutableStateOf<List<ChildProfile>>(emptyList()) }
    var weeklyThemes by remember { mutableStateOf<List<WeeklyTheme>>(emptyList()) }
    var apiKey by remember { mutableStateOf("") }
    
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Profils, 1 = Thèmes, 2 = Clé API

    val database = remember { FirebaseDatabase.getInstance() }

    // Charger les données de Firebase
    LaunchedEffect(familyCode) {
        if (familyCode.isNotBlank()) {
            val familyRef = database.getReference("families").child(familyCode)

            // Profils Enfants
            familyRef.child("children").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<ChildProfile>()
                    for (child in snapshot.children) {
                        list.add(
                            ChildProfile(
                                id = child.child("id").getValue(String::class.java) ?: "",
                                name = child.child("name").getValue(String::class.java) ?: "",
                                birthDateMillis = (child.child("birthDateMillis").value as? Long) ?: 0L
                            )
                        )
                    }
                    childrenProfiles = list
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            // Thèmes Histoires
            familyRef.child("weekly_themes").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val list = mutableListOf<WeeklyTheme>()
                    for (child in snapshot.children) {
                        list.add(
                            WeeklyTheme(
                                dayOfWeek = (child.child("dayOfWeek").value as? Long)?.toInt() ?: 1,
                                label = child.child("label").getValue(String::class.java) ?: "",
                                promptTheme = child.child("promptTheme").getValue(String::class.java) ?: ""
                            )
                        )
                    }
                    weeklyThemes = list
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            // Clé API Groq
            familyRef.child("ai_settings").child("apiKey").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    apiKey = snapshot.getValue(String::class.java) ?: ""
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TabRow(selectedTabIndex = selectedTab, modifier = Modifier.fillMaxWidth()) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("👧 Profils", modifier = Modifier.padding(16.dp))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("📚 Thèmes", modifier = Modifier.padding(16.dp))
            }
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) {
                Text("🤖 IA (Groq)", modifier = Modifier.padding(16.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        when (selectedTab) {
            0 -> ManageChildrenProfiles(childrenProfiles) { newChild ->
                val updatedList = childrenProfiles + newChild
                database.getReference("families").child(familyCode).child("children").setValue(updatedList)
            }
            1 -> ManageWeeklyThemes(weeklyThemes) { newTheme ->
                val updatedList = weeklyThemes.filter { it.dayOfWeek != newTheme.dayOfWeek } + newTheme
                database.getReference("families").child(familyCode).child("weekly_themes").setValue(updatedList.sortedBy { it.dayOfWeek })
            }
            2 -> ManageAiSettings(apiKey) { newKey ->
                database.getReference("families").child(familyCode).child("ai_settings").child("apiKey").setValue(newKey)
            }
        }
    }
}

@Composable
fun ManageChildrenProfiles(profiles: List<ChildProfile>, onAddChild: (ChildProfile) -> Unit) {
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("Ajouter un Enfant")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(profiles) { child ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("👧", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(child.name, fontWeight = FontWeight.Bold)
                        Text("Âge : ${child.getAgeInYears()} ans", color = Color.Gray)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        // Simplification pour l'exemple : on fixe l'âge en dur lors de la saisie (à améliorer avec DatePicker)
        var ageStr by remember { mutableStateOf("4") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nouvel Enfant") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Prénom") })
                    OutlinedTextField(value = ageStr, onValueChange = { ageStr = it }, label = { Text("Âge (en années)") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    val age = ageStr.toIntOrNull() ?: 4
                    val birthDate = System.currentTimeMillis() - (age * 365L * 24 * 60 * 60 * 1000)
                    onAddChild(ChildProfile(name = name, birthDateMillis = birthDate))
                    showAddDialog = false
                }) { Text("Enregistrer") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Annuler") } }
        )
    }
}

@Composable
fun ManageWeeklyThemes(themes: List<WeeklyTheme>, onUpdateTheme: (WeeklyTheme) -> Unit) {
    val days = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("Les 7 Thèmes de la Semaine", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text("Ils apparaîtront sous forme de 7 boutons sur le téléphone de l'enfant.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(7) { index ->
            val dayNumber = index + 1
            val existingTheme = themes.find { it.dayOfWeek == dayNumber }
            
            var isEditing by remember { mutableStateOf(false) }
            var label by remember { mutableStateOf(existingTheme?.label ?: "") }
            var prompt by remember { mutableStateOf(existingTheme?.promptTheme ?: "") }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(days[index], fontWeight = FontWeight.Bold)
                    
                    if (isEditing) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Titre du bouton (ex: Aventure Spatiale)") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(value = prompt, onValueChange = { prompt = it }, label = { Text("Thème détaillé pour l'IA") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { isEditing = false }) { Text("Annuler") }
                            Button(onClick = {
                                onUpdateTheme(WeeklyTheme(dayOfWeek = dayNumber, label = label, promptTheme = prompt))
                                isEditing = false
                            }) { Text("Enregistrer") }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Titre : ${existingTheme?.label ?: "Non défini"}")
                        Text("Thème IA : ${existingTheme?.promptTheme ?: "Non défini"}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { isEditing = true }) {
                            Text("Modifier le thème du ${days[index]}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ManageAiSettings(currentKey: String, onSaveKey: (String) -> Unit) {
    var keyInput by remember { mutableStateOf(currentKey) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Paramètres Intelligence Artificielle", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Entrez votre clé API secrète (Groq). Elle servira à générer les histoires automatiquement depuis l'appareil de l'enfant.")
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it },
            label = { Text("Clé API Groq (gsk_...)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onSaveKey(keyInput) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Key, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sauvegarder la Clé Sécurisée")
        }
    }
}
