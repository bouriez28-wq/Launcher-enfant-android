package com.example.launchertlphoneenfant.parent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.launchertlphoneenfant.parent.ui.screens.ParentControlScreen
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
                title = { Text("Parent Contrôle", fontWeight = FontWeight.Bold) },
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
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Contrôle") },
                    label = { Text("Contrôle") }
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
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            when (selectedTab) {
                0 -> ParentControlScreen(
                    familyCode = familyCode,
                    onFamilyCodeChange = { familyCode = it },
                    dailyLimit = dailyLimit,
                    usedMinutes = usedMinutes,
                    bonusMinutes = bonusMinutes,
                    onSetDailyLimit = { limit ->
                        database.getReference("families").child(familyCode).child("dailyLimitMinutes").setValue(limit)
                    },
                    onAddBonusMinutes = { bonus ->
                        database.getReference("families").child(familyCode).child("bonusMinutesToday").setValue(bonusMinutes + bonus)
                    },
                    onLockDevice = {
                        database.getReference("families").child(familyCode).child("dailyLimitMinutes").setValue(0)
                    },
                    onUnlockDevice = {
                        database.getReference("families").child(familyCode).child("dailyLimitMinutes").setValue(60)
                    }
                )
                1 -> ParentValidationScreen()
                2 -> ParentRoutinesScreen()
                3 -> ParentStoriesScreen()
            }
        }
    }
}

