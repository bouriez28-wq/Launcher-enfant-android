package com.example.launchertlphoneenfant

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.launchertlphoneenfant.domain.KioskManager
import com.example.launchertlphoneenfant.service.TimeLimitOverlayService
import com.example.launchertlphoneenfant.ui.screens.ChildHomeScreen
import com.example.launchertlphoneenfant.ui.screens.ParentDashboardScreen
import com.example.launchertlphoneenfant.ui.screens.RoutinesScreen
import com.example.launchertlphoneenfant.ui.screens.StoriesScreen
import com.example.launchertlphoneenfant.ui.screens.TimeLimitLockScreen
import com.example.launchertlphoneenfant.ui.theme.AnimeLauncherTheme
import com.example.launchertlphoneenfant.ui.viewmodel.ChildLauncherViewModel
import com.example.launchertlphoneenfant.ui.viewmodel.ParentViewModel

class MainActivity : ComponentActivity() {

    private val childViewModel: ChildLauncherViewModel by viewModels()
    private val parentViewModel: ParentViewModel by viewModels()
    private val kioskManager = KioskManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Demande d'autorisation de notifications PUSH sur Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Enregistrement du Token FCM pour réveiller le téléphone Enfant
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                if (!token.isNullOrBlank()) {
                    com.google.firebase.database.FirebaseDatabase.getInstance("https://launcher-enfant-default-rtdb.europe-west1.firebasedatabase.app")
                        .getReference("families").child("FAM-8492").child("childFcmToken").setValue(token)
                }
            }
        }

        // Démarre le service de surveillance du temps d'écran
        val serviceIntent = Intent(this, TimeLimitOverlayService::class.java)
        startService(serviceIntent)

        // Active le mode Kiosque (Lock Task)
        kioskManager.startKioskMode(this)

        setContent {
            val childState by childViewModel.uiState.collectAsState()
            val parentState by parentViewModel.uiState.collectAsState()
            val navController = rememberNavController()

            val showLockScreen = intent?.getBooleanExtra("SHOW_LOCK_SCREEN", false) ?: false

            AnimeLauncherTheme(themeName = childState.currentTheme) {
                LaunchedEffect(childState.isTimeUp, showLockScreen) {
                    if (childState.isTimeUp || showLockScreen) {
                        navController.navigate("time_limit_lock") {
                            popUpTo(0)
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = if (childState.isTimeUp || showLockScreen) "time_limit_lock" else "child_home"
                ) {
                    composable("child_home") {
                        ChildHomeScreen(
                            uiState = childState,
                            onAppClick = { packageName ->
                                if (!childState.isTimeUp) {
                                    childViewModel.launchApp(packageName)
                                } else {
                                    navController.navigate("time_limit_lock")
                                }
                            },
                            onOpenRoutines = {
                                navController.navigate("routines")
                            },
                            onOpenStories = {
                                navController.navigate("stories")
                            },
                            onOpenParentDashboard = {
                                navController.navigate("parent_dashboard")
                            }
                        )
                    }

                    composable("routines") {
                        RoutinesScreen(
                            routines = childState.routines,
                            onCompleteRoutine = { routine ->
                                childViewModel.completeRoutine(routine)
                            },
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("stories") {
                        StoriesScreen(
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("parent_dashboard") {
                        ParentDashboardScreen(
                            uiState = parentState,
                            onToggleAppHide = { pkg, name, hide ->
                                parentViewModel.toggleAppVisibility(pkg, name, hide)
                                childViewModel.refreshData()
                            },
                            onSetDailyLimit = { limit ->
                                parentViewModel.setDailyLimit(limit)
                                childViewModel.refreshData()
                            },
                            onAddBonusMinutes = { bonus ->
                                parentViewModel.addBonusMinutes(bonus)
                                childViewModel.refreshData()
                            },
                            onAddRoutine = { title, desc, reward ->
                                parentViewModel.addRoutine(title, desc, reward)
                                childViewModel.refreshData()
                            },
                            onDeleteRoutine = { routine ->
                                parentViewModel.deleteRoutine(routine)
                                childViewModel.refreshData()
                            },
                            onSetTheme = { theme ->
                                parentViewModel.setSelectedTheme(theme)
                                childViewModel.refreshData()
                            },
                            onSetPin = { newPin ->
                                parentViewModel.setParentPin(newPin)
                            },
                            onSetFamilyCode = { code ->
                                parentViewModel.setFamilyCode(code)
                                childViewModel.refreshData()
                            },
                            onExitKiosk = {
                                kioskManager.stopKioskMode(this@MainActivity)
                            },
                            onBack = {
                                navController.navigate("child_home") {
                                    popUpTo("child_home") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("time_limit_lock") {
                        TimeLimitLockScreen(
                            onUnlockWithPin = { pin ->
                                parentViewModel.verifyPin(pin)
                            },
                            onOpenParentDashboard = {
                                navController.navigate("parent_dashboard") {
                                    popUpTo("child_home") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        childViewModel.refreshData()
    }
}
