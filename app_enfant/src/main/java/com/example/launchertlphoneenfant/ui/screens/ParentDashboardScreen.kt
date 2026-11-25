package com.example.launchertlphoneenfant.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.example.launchertlphoneenfant.ui.viewmodel.ParentUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    uiState: ParentUiState,
    onToggleAppHide: (packageName: String, appName: String, hide: Boolean) -> Unit,
    onSetDailyLimit: (Int) -> Unit,
    onAddBonusMinutes: (Int) -> Unit,
    onAddRoutine: (title: String, description: String, rewardMinutes: Int) -> Unit,
    onDeleteRoutine: (com.example.launchertlphoneenfant.data.model.Routine) -> Unit,
    onSetTheme: (String) -> Unit,
    onSetPin: (String) -> Unit,
    onSetFamilyCode: (String) -> Unit,
    onExitKiosk: () -> Unit,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Applications", "Temps", "Routines", "Réglages")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Espace Contrôle Parental", fontWeight = FontWeight.Bold) },
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
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.SemiBold) },
                        icon = {
                            when (index) {
                                0 -> Icon(Icons.Default.Apps, contentDescription = null)
                                1 -> Icon(Icons.Default.Schedule, contentDescription = null)
                                2 -> Icon(Icons.Default.Star, contentDescription = null)
                                3 -> Icon(Icons.Default.Palette, contentDescription = null)
                            }
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> AppsManagementTab(uiState = uiState, onToggleAppHide = onToggleAppHide)
                1 -> TimeManagementTab(uiState = uiState, onSetDailyLimit = onSetDailyLimit, onAddBonusMinutes = onAddBonusMinutes)
                2 -> RoutinesManagementTab(uiState = uiState, onAddRoutine = onAddRoutine, onDeleteRoutine = onDeleteRoutine)
                3 -> SettingsTab(uiState = uiState, onSetTheme = onSetTheme, onSetPin = onSetPin, onSetFamilyCode = onSetFamilyCode, onExitKiosk = onExitKiosk)
            }
        }
    }
}

@Composable
fun AppsManagementTab(
    uiState: ParentUiState,
    onToggleAppHide: (packageName: String, appName: String, hide: Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Masquer des applications pour l'enfant :",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(uiState.allApps) { app ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val bitmap = app.icon?.toBitmap()
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = app.appName,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Text(
                        text = app.appName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = if (app.isHidden) "Masquée" else "Visible",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    Switch(
                        checked = app.isHidden,
                        onCheckedChange = { isChecked ->
                            onToggleAppHide(app.packageName, app.appName, isChecked)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TimeManagementTab(
    uiState: ParentUiState,
    onSetDailyLimit: (Int) -> Unit,
    onAddBonusMinutes: (Int) -> Unit
) {
    var limitSliderValue by remember(uiState.dailyLimitMinutes) {
        mutableStateOf(uiState.dailyLimitMinutes.toFloat())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Temps d'écran quotidien global",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Limite quotidienne : ${limitSliderValue.toInt()} minutes (${limitSliderValue.toInt() / 60}h ${limitSliderValue.toInt() % 60}m)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = limitSliderValue,
                    onValueChange = { limitSliderValue = it },
                    onValueChangeFinished = { onSetDailyLimit(limitSliderValue.toInt()) },
                    valueRange = 0f..240f,
                    steps = 15
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Ajouter du temps bonus aujourd'hui",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onAddBonusMinutes(15) },
                modifier = Modifier.weight(1f)
            ) {
                Text("+15 min")
            }
            Button(
                onClick = { onAddBonusMinutes(30) },
                modifier = Modifier.weight(1f)
            ) {
                Text("+30 min")
            }
            Button(
                onClick = { onAddBonusMinutes(60) },
                modifier = Modifier.weight(1f)
            ) {
                Text("+1 heure")
            }
        }
    }
}

@Composable
fun RoutinesManagementTab(
    uiState: ParentUiState,
    onAddRoutine: (String, String, Int) -> Unit,
    onDeleteRoutine: (com.example.launchertlphoneenfant.data.model.Routine) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var rewardText by remember { mutableStateOf("15") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Ajouter une nouvelle routine",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre de la tâche (ex: Ranger sa chambre)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = rewardText,
                    onValueChange = { rewardText = it },
                    label = { Text("Bonus de temps de jeu (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        val reward = rewardText.toIntOrNull() ?: 15
                        if (title.isNotBlank()) {
                            onAddRoutine(title, "", reward)
                            title = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ajouter la routine")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Routines existantes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.routines) { routine ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(routine.title, fontWeight = FontWeight.Bold)
                            Text("+${routine.rewardMinutes} min", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { onDeleteRoutine(routine) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTab(
    uiState: ParentUiState,
    onSetTheme: (String) -> Unit,
    onSetPin: (String) -> Unit,
    onSetFamilyCode: (String) -> Unit,
    onExitKiosk: () -> Unit
) {
    var newPin by remember { mutableStateOf("") }
    var newFamilyCode by remember { mutableStateOf(uiState.familyCode) }
    val themes = listOf("SHONEN", "CYBERPUNK", "CHIBI", "PASTEL")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Bouton Quitter le Kiosque TOUT EN HAUT
        Button(
            onClick = onExitKiosk,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Default.Lock, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Quitter le mode Kiosque (Désépingler)", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Synchronisation à Distance (Cloud)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Code Famille : ${uiState.familyCode}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newFamilyCode,
                    onValueChange = { newFamilyCode = it },
                    label = { Text("Code d'appairage parent (ex: FAM-8492)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (newFamilyCode.isNotBlank()) {
                            onSetFamilyCode(newFamilyCode)
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Lier au Cloud Parent")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Thème Animé",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            themes.forEach { theme ->
                val isSelected = uiState.selectedTheme.equals(theme, ignoreCase = true)
                Card(
                    onClick = { onSetTheme(theme) },
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = theme,
                        modifier = Modifier.padding(12.dp),
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Changer le code PIN Parent",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4) newPin = it },
                    label = { Text("Nouveau PIN (4 chiffres)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (newPin.length == 4) {
                            onSetPin(newPin)
                            newPin = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Enregistrer le PIN")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onExitKiosk,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Quitter le mode Kiosque (Désépingler)")
        }
    }
}
