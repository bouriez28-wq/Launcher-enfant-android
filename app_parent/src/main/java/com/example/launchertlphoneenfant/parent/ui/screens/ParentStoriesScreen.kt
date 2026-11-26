package com.example.launchertlphoneenfant.parent.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.launchertlphoneenfant.parent.model.ChildProfile
import com.example.launchertlphoneenfant.parent.model.WeeklyTheme
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar

@Composable
fun ParentStoriesScreen(familyCode: String) {
    var childrenProfiles by remember { mutableStateOf<List<ChildProfile>>(emptyList()) }
    var weeklyThemes by remember { mutableStateOf<List<WeeklyTheme>>(emptyList()) }
    var currentMainTheme by remember { mutableStateOf("Animaux") }
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

            // Grand Thème de la semaine en cours
            familyRef.child("currentMainTheme").addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    currentMainTheme = snapshot.getValue(String::class.java) ?: "Animaux"
                }
                override fun onCancelled(error: DatabaseError) {}
            })

            // Thèmes Histoires (7 jours)
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
            1 -> ManageWeeklyThemes(weeklyThemes, currentMainTheme, familyCode, database)
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

val baseMainThemes = mapOf(
    "Animaux" to listOf("Le lion courageux", "L'ours et le miel", "Le renard malin", "Le petit écureuil", "Le loup musicien", "Le hibou sage", "La gazelle rapide"),
    "Magie" to listOf("L'école des sorciers", "La baguette étincellante", "Le grimoire secret", "La potion arc-en-ciel", "Le balai volant", "Le château flottant", "Le cristal bleu"),
    "Espace" to listOf("Le voyage sur Mars", "La ronde des étoiles", "La fusée pailletée", "Le petit cosmonaute", "La comète voyageuse", "La station lunaire", "Les anneaux de Saturne"),
    "Océan" to listOf("Le dauphin joyeux", "Le trésor des coraux", "L'île mystérieuse", "La baleine chanteuse", "Le sous-marin jaune", "Les poissons lumineux", "La sirène curieuse"),
    "Dinosaures" to listOf("Le petit diplodocus", "L'œuf mystérieux", "Le volcan endormi", "La vallée secrète", "Le tricératops joueur", "Le ptéranodon voyageur", "Le roi T-Rex"),
    "Princesse" to listOf("Le château enchanté", "La couronne étincelante", "Le bal royal", "Le jardin secret", "Le carrosse de verre", "Le dragon gardien", "L'amitié sincère"),
    "Super-héros" to listOf("Les super-pouvoirs", "La ville endormie", "L'équipe de choc", "La cape magique", "Le QG secret", "Le sauvetage rigolo", "Le bouclier d'étoiles"),
    "Nature" to listOf("La grande forêt", "La rivière chantante", "La montagne dorée", "La prairie aux fleurs", "Le vent d'automne", "Le premier bourgeon", "Le soleil d'or"),
    "Chevaliers" to listOf("Le bouclier d'argent", "Le tournoi royal", "Le fier destrier", "La quête du Graal", "Le pont suspendu", "La tour de garde", "Le chevalier pacifique"),
    "Voyage" to listOf("Le train magique", "Le ballon à air chaud", "Le tour du monde", "Le désert d'étoiles", "Le pays du soleil", "L'archipel secret", "Le grand périple")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageWeeklyThemes(themes: List<WeeklyTheme>, currentMainTheme: String, familyCode: String, database: FirebaseDatabase) {
    val context = LocalContext.current
    val days = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")

    // Calculer automatiquement le thème de la semaine par roulement (semaine de l'année)
    val calendar = Calendar.getInstance()
    val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
    val mainThemeKeys = baseMainThemes.keys.toList()
    val automaticMainTheme = mainThemeKeys[weekOfYear % mainThemeKeys.size]

    var selectedMainTheme by remember(currentMainTheme) { mutableStateOf(currentMainTheme.ifBlank { automaticMainTheme }) }
    var expandedDropdown by remember { mutableStateOf(false) }

    val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val todayIndex = if (currentDayOfWeek == Calendar.SUNDAY) 6 else currentDayOfWeek - 2

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. Sélection du Grand Thème de la Semaine (Automatique ou Forcé par les parents)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "🎡 Grand Thème Principal de la Semaine", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Rotation automatique hebdomadaire (ou choix manuel par les parents).", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))

                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedMainTheme,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Thème Principal") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            baseMainThemes.keys.forEach { themeKey ->
                                DropdownMenuItem(
                                    text = { Text(themeKey) },
                                    onClick = {
                                        selectedMainTheme = themeKey
                                        expandedDropdown = false
                                        // Sauvegarder le thème principal et générer automatiquement ses 7 sous-thèmes
                                        database.getReference("families").child(familyCode).child("currentMainTheme").setValue(themeKey)
                                        val subList = baseMainThemes[themeKey] ?: emptyList()
                                        val newWeeklyThemes = subList.mapIndexed { idx, subTitle ->
                                            WeeklyTheme(dayOfWeek = idx + 1, label = subTitle, promptTheme = "Une histoire sur le thème de $themeKey : $subTitle")
                                        }
                                        database.getReference("families").child(familyCode).child("weekly_themes").setValue(newWeeklyThemes)
                                        Toast.makeText(context, "Thème de la semaine appliqué : $themeKey", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Les 7 Sous-Thèmes Quotidiens (Modifiables)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
        }

        // 2. Liste des 7 sous-thèmes (Lundi à Dimanche) avec accordéon / édition
        items(7) { index ->
            val dayNumber = index + 1
            val existingTheme = themes.find { it.dayOfWeek == dayNumber }
            
            var isEditing by remember { mutableStateOf(false) }
            var label by remember { mutableStateOf(existingTheme?.label ?: "") }
            var prompt by remember { mutableStateOf(existingTheme?.promptTheme ?: "") }

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(days[index], fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        if (index == todayIndex) {
                            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
                                Text("Aujourd'hui", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    
                    if (isEditing) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Titre du sous-thème") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(value = prompt, onValueChange = { prompt = it }, label = { Text("Description IA") }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { isEditing = false }) { Text("Annuler") }
                            Button(onClick = {
                                val updatedTheme = WeeklyTheme(dayOfWeek = dayNumber, label = label.trim(), promptTheme = prompt.trim())
                                val updatedList = themes.filter { it.dayOfWeek != dayNumber } + updatedTheme
                                database.getReference("families").child(familyCode).child("weekly_themes").setValue(updatedList.sortedBy { it.dayOfWeek })
                                isEditing = false
                                Toast.makeText(context, "Sous-thème mis à jour !", Toast.LENGTH_SHORT).show()
                            }) { Text("Enregistrer") }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Titre : ${existingTheme?.label ?: "Non défini"}")
                        Text("Thème IA : ${existingTheme?.promptTheme ?: "Non défini"}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = {
                            label = existingTheme?.label ?: ""
                            prompt = existingTheme?.promptTheme ?: ""
                            isEditing = true
                        }) {
                            Text("Modifier ce sous-thème")
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
