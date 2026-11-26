package com.example.launchertlphoneenfant.parent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LibraryBooks
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.launchertlphoneenfant.parent.ui.screens.ParentHomeScreen
import com.example.launchertlphoneenfant.parent.ui.screens.ParentRoutinesScreen
import com.example.launchertlphoneenfant.parent.ui.screens.ParentStoriesScreen
import com.example.launchertlphoneenfant.parent.ui.screens.ParentValidationScreen
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

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

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ParentAppNavigation()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentAppNavigation() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val pagerState = rememberPagerState(pageCount = { 4 })

    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            pagerState.animateScrollToPage(selectedTab)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        selectedTab = pagerState.currentPage
    }

    // État synchronisé depuis Firebase
    var familyCode by remember { mutableStateOf("FAM-8492") }
    var dailyLimit by remember { mutableIntStateOf(60) }
    var bonusMinutes by remember { mutableIntStateOf(0) }
    var usedMinutes by remember { mutableIntStateOf(0) }

    val databaseUrl = "https://launcher-enfant-default-rtdb.europe-west1.firebasedatabase.app"
    val database = remember {
        try {
            FirebaseDatabase.getInstance(databaseUrl)
        } catch (e: Exception) {
            FirebaseDatabase.getInstance()
        }
    }

    remember(familyCode) {
        if (familyCode.isNotBlank()) {
            val ref = database.getReference("families").child(familyCode)
            ref.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        dailyLimit = (snapshot.child("dailyLimitMinutes").value as? Long)?.toInt() ?: 60
                        bonusMinutes = (snapshot.child("bonusMinutesToday").value as? Long)?.toInt() ?: 0
                        usedMinutes = (snapshot.child("usedMinutesToday").value as? Long)?.toInt() ?: 0
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

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
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Accueil") },
                    label = { Text("Accueil") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Checklist, contentDescription = "Validation") },
                    label = { Text("Validation") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Routines") },
                    label = { Text("Routines") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.LibraryBooks, contentDescription = "Histoires") },
                    label = { Text("Histoires") }
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
                    0 -> ParentHomeScreen(
                        familyCode = familyCode,
                        onFamilyCodeChange = { familyCode = it }
                    )
                    1 -> ParentValidationScreen(familyCode = familyCode)
                    2 -> ParentRoutinesScreen(familyCode = familyCode)
                    3 -> ParentStoriesScreen(familyCode = familyCode)
                }
            }
        }
    }
}
