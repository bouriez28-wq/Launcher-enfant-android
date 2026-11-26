package com.example.launchertlphoneenfant.parent

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.launchertlphoneenfant.parent.data.ApkInstaller
import com.example.launchertlphoneenfant.parent.service.UpdateCheckWorker
import com.example.launchertlphoneenfant.parent.ui.screens.ParentHomeScreen
import com.example.launchertlphoneenfant.parent.ui.screens.ParentRoutinesScreen
import com.example.launchertlphoneenfant.parent.ui.screens.ParentSettingsScreen
import com.example.launchertlphoneenfant.parent.ui.screens.ParentStoriesScreen
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class ParentMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Demande d'autorisation de notifications PUSH sur Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 102)
            }
        }

        // Enregistrement du Token FCM pour réveiller le téléphone Parent
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                if (!token.isNullOrBlank()) {
                    FirebaseDatabase.getInstance("https://launcher-enfant-default-rtdb.europe-west1.firebasedatabase.app")
                        .getReference("families").child("FAM-8492").child("parentFcmToken").setValue(token)
                }
            }
        }

        // Planification de la vérification des mises à jour en arrière-plan (WorkManager - Périodique toutes les 4h)
        val updateWorkRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(4, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "UpdateCheckWork",
            ExistingPeriodicWorkPolicy.KEEP,
            updateWorkRequest
        )

        // Gérer le clic sur la notification de mise à jour s'il y en a un
        handleUpdateIntent(intent)

        setContent {
            val prefs = remember { getSharedPreferences("parent_theme_prefs", Context.MODE_PRIVATE) }
            val colorPalette = prefs.getString("color_palette", "Orange Chaud") ?: "Orange Chaud"
            val themeMode = prefs.getString("theme_mode", "Système") ?: "Système"

            val darkTheme = when (themeMode) {
                "Sombre" -> true
                "Clair" -> false
                else -> isSystemInDarkTheme()
            }

            val primaryColor = when (colorPalette) {
                "Bleu Océan" -> Color(0xFF0288D1)
                "Vert Forêt" -> Color(0xFF2E7D32)
                "Violet Magique" -> Color(0xFF7B1FA2)
                "Rose Doux" -> Color(0xFFC2185B)
                "Rouge Passion" -> Color(0xFFD32F2F)
                "Jaune Soleil" -> Color(0xFFFBC02D)
                "Gris Sobre" -> Color(0xFF455A64)
                else -> Color(0xFFFF6D00)
            }

            val colorScheme = if (darkTheme) {
                darkColorScheme(primary = primaryColor)
            } else {
                lightColorScheme(primary = primaryColor)
            }

            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ParentAppNavigation()
                }
            }
        }
    }

    private fun handleUpdateIntent(intent: Intent?) {
        val downloadUrl = intent?.getStringExtra("download_url")
        if (!downloadUrl.isNullOrBlank()) {
            val apkInstaller = ApkInstaller(this)
            lifecycleScope.launch(Dispatchers.IO) {
                apkInstaller.downloadAndInstallApk(downloadUrl) { _ -> }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleUpdateIntent(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentAppNavigation() {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()

    // État synchronisé depuis Firebase
    var familyCode by remember { mutableStateOf("FAM-8492") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Parent Contrôle", fontWeight = FontWeight.Bold)
                        Text("Version v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = pagerState.currentPage == 0,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Accueil") },
                    label = { Text("Accueil") }
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 1,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(1) } },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Routines") },
                    label = { Text("Routines") }
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 2,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(2) } },
                    icon = { Icon(Icons.Default.LibraryBooks, contentDescription = "Histoires") },
                    label = { Text("Histoires") }
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 3,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(3) } },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Paramètres") },
                    label = { Text("Paramètres") }
                )
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) { page ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (page) {
                    0 -> ParentHomeScreen(familyCode = familyCode)
                    1 -> ParentRoutinesScreen(familyCode = familyCode)
                    2 -> ParentStoriesScreen(familyCode = familyCode)
                    3 -> ParentSettingsScreen(
                        familyCode = familyCode,
                        onFamilyCodeChange = { familyCode = it }
                    )
                }
            }
        }
    }
}
