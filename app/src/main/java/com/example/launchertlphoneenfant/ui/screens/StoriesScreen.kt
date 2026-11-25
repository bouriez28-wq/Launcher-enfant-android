package com.example.launchertlphoneenfant.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.launchertlphoneenfant.data.model.Story

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
    var selectedStory by remember { mutableStateOf<Story?>(null) }

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
                        .height(300.dp)
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
