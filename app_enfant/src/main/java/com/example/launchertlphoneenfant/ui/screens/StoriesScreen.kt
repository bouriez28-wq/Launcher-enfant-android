package com.example.launchertlphoneenfant.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.launchertlphoneenfant.data.model.Story
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

val defaultStories = listOf(
    Story(
        id = "1",
        title = "Le Petit Dragon Poussiéreux",
        content = "Il était une fois un tout petit dragon qui ne savait pas encore cracher du feu, mais seulement des bulles de savon dorées. Chaque soir, il volait au-dessus du village pour offrir de doux rêves à tous les enfants endormis...",
        durationMinutes = 5,
        category = "Magie",
        iconEmoji = "🐉"
    ),
    Story(
        id = "2",
        title = "Le Voyage vers la Lune de Sucre",
        content = "Chaque nuit, quand les étoiles s'allument, un petit train bleu traverse les nuages en chantant. Son terminus ? La Lune de Sucre, où pousse le chocolat le plus doux du monde...",
        durationMinutes = 7,
        category = "Espace",
        iconEmoji = "🌙"
    ),
    Story(
        id = "3",
        title = "La Forêt Enchantée des Animaux",
        content = "Au cœur de la forêt, les petits lapins et les faons se réunissent autour du Grand Chêne pour écouter les hiboux raconter les légendes du monde ancien...",
        durationMinutes = 6,
        category = "Nature",
        iconEmoji = "🌲"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoriesScreen(
    stories: List<Story> = defaultStories,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedStory by remember { mutableStateOf<Story?>(null) }
    var showAiDialog by remember { mutableStateOf(false) }
    var isGenerating by remember { mutableStateOf(false) }

    var apiKey by remember { mutableStateOf("") }
    var childName by remember { mutableStateOf("Mon Champion") }
    var todayThemeLabel by remember { mutableStateOf("une aventure magique") }
    var todayThemePrompt by remember { mutableStateOf("une aventure féerique dans un royaume enchanté") }

    val database = remember { FirebaseDatabase.getInstance() }

    // Charger la clé API, le prénom de l'enfant et le thème du jour depuis Firebase
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("family_prefs", Context.MODE_PRIVATE)
        val familyCode = prefs.getString("family_code", "FAM-8492") ?: "FAM-8492"
        val familyRef = database.getReference("families").child(familyCode)

        familyRef.child("ai_settings").child("apiKey").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                apiKey = snapshot.getValue(String::class.java) ?: ""
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        familyRef.child("childName").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                childName = snapshot.getValue(String::class.java) ?: "Mon Champion"
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        familyRef.child("weekly_themes").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val calendar = Calendar.getInstance()
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val dayIndex = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1

                for (child in snapshot.children) {
                    val dNum = (child.child("dayOfWeek").value as? Long)?.toInt() ?: 1
                    if (dNum == dayIndex) {
                        todayThemeLabel = child.child("label").getValue(String::class.java) ?: todayThemeLabel
                        todayThemePrompt = child.child("promptTheme").getValue(String::class.java) ?: todayThemePrompt
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histoires du Soir 📖", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Carte pour générer une histoire avec l'IA Groq
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (apiKey.isBlank()) {
                            Toast.makeText(context, "Aucune clé API IA configurée par les parents dans l'onglet Paramètres/Histoires.", Toast.LENGTH_LONG).show()
                        } else {
                            showAiDialog = true
                        }
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("✨ Créer une histoire magique avec l'IA", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Thème du jour : $todayThemeLabel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Choisis une histoire pour t'endormir paisiblement ✨",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(stories) { story ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStory = story },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = story.iconEmoji, fontSize = 32.sp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(story.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("${story.durationMinutes} min de lecture • ${story.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            }
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    // Dialogue de génération d'histoire par IA
    if (showAiDialog) {
        var customTheme by remember { mutableStateOf(todayThemePrompt) }
        var selectedMood by remember { mutableStateOf("Doux et apaisant") }

        AlertDialog(
            onDismissRequest = { if (!isGenerating) showAiDialog = false },
            title = { Text("✨ Création d'une histoire magique", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Héros de l'histoire : $childName", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)

                    OutlinedTextField(
                        value = customTheme,
                        onValueChange = { customTheme = it },
                        label = { Text("Thème de l'histoire") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Ambiance de l'histoire :", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Doux et apaisant", "Magique", "Joyeux", "Mystérieux").forEach { mood ->
                            FilterChip(
                                selected = selectedMood == mood,
                                onClick = { selectedMood = mood },
                                label = { Text(mood, fontSize = 11.sp) }
                            )
                        }
                    }

                    if (isGenerating) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("L'IA écrit ton histoire magique...", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            },
            confirmButton = {
                if (!isGenerating) {
                    Button(onClick = {
                        isGenerating = true
                        coroutineScope.launch {
                            val generatedStory = generateStoryWithGroq(apiKey, childName, customTheme, selectedMood)
                            isGenerating = false
                            showAiDialog = false
                            if (generatedStory != null) {
                                selectedStory = generatedStory
                            } else {
                                Toast.makeText(context, "Erreur lors de la génération de l'histoire.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }) {
                        Text("Générer mon histoire")
                    }
                }
            },
            dismissButton = {
                if (!isGenerating) {
                    TextButton(onClick = { showAiDialog = false }) { Text("Annuler") }
                }
            }
        )
    }

    if (selectedStory != null) {
        val story = selectedStory!!
        AlertDialog(
            onDismissRequest = { selectedStory = null },
            title = {
                Text(text = "${story.iconEmoji} ${story.title}", fontWeight = FontWeight.Bold)
            },
            text = {
                Box(
                    modifier = Modifier
                        .height(350.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = story.content,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 26.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedStory = null }) {
                    Text("Fermer l'histoire")
                }
            }
        )
    }
}

// Fonction de génération d'histoire via l'API Groq (exactement comme dans l'app d'origine)
suspend fun generateStoryWithGroq(apiKey: String, childName: String, theme: String, mood: String): Story? = withContext(Dispatchers.IO) {
    try {
        val prompt = """
            Génère une histoire pour enfants pour le coucher où le héros est exclusivement l'enfant suivant : $childName.
            Thème : $theme.
            Ambiance : $mood.
            Longueur : 250-350 mots.

            Contraintes obligatoires :
            1) Garde un style fluide, naturel, poétique et adapté pour s'endormir paisiblement.
            2) Utilise le prénom $childName tout au long de l'histoire.
            3) Le texte doit être 100% en français (aucun mot en anglais).
            4) Respecte strictement la grammaire et la syntaxe françaises.

            Réponds UNIQUEMENT au format JSON strict : {"titre":"", "histoire":""}
        """.trimIndent()

        val url = URL("https://api.groq.com/openai/v1/chat/completions")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            connectTimeout = 20000
            readTimeout = 20000
        }

        val requestBody = JSONObject().apply {
            put("model", "llama-3.3-70b-versatile")
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", prompt)
            }))
            put("response_format", JSONObject().put("type", "json_object"))
        }

        connection.outputStream.use { os ->
            val input = requestBody.toString().toByteArray(Charsets.UTF_8)
            os.write(input, 0, input.size)
        }

        val responseCode = connection.responseCode
        if (responseCode in 200..299) {
            val responseString = connection.inputStream.bufferedReader().use { it.readText() }
            val jsonResponse = JSONObject(responseString)
            val choices = jsonResponse.getJSONArray("choices")
            val messageContent = choices.getJSONObject(0).getJSONObject("message").getString("content")

            val storyJson = JSONObject(messageContent)
            val title = storyJson.getString("titre")
            val content = storyJson.getString("histoire")

            return@withContext Story(
                id = System.currentTimeMillis().toString(),
                title = title,
                content = content,
                durationMinutes = 5,
                category = "Magie IA",
                iconEmoji = "✨"
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return@withContext null
}
