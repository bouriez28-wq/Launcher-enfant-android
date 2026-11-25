package com.example.launchertlphoneenfant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.launchertlphoneenfant.MainActivity
import com.example.launchertlphoneenfant.R
import com.example.launchertlphoneenfant.data.NotificationHelper
import com.example.launchertlphoneenfant.data.UpdateChecker
import com.example.launchertlphoneenfant.data.preferences.ParentPreferencesRepository
import com.example.launchertlphoneenfant.domain.UsageStatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimeLimitOverlayService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var usageStatsRepository: UsageStatsRepository
    private lateinit var preferencesRepository: ParentPreferencesRepository
    private lateinit var notificationHelper: NotificationHelper
    private val updateChecker = UpdateChecker()

    override fun onCreate() {
        super.onCreate()
        usageStatsRepository = UsageStatsRepository(this)
        preferencesRepository = ParentPreferencesRepository(this)
        notificationHelper = NotificationHelper(this)

        createNotificationChannel()
        startForegroundServiceNotification()
        startMonitoringTime()
        startBackgroundUpdateCheck()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Contrôle du Temps d'Écran",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification active du contrôle parental du temps d'écran"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceNotification() {
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Launcher Enfant Actif")
            .setContentText("Surveillance du temps d'écran en cours...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startMonitoringTime() {
        serviceScope.launch {
            while (isActive) {
                delay(30_000) // Vérification toutes les 30 secondes

                val usedMinutes = usageStatsRepository.getTodayUsageTimeMinutes()
                val dailyLimit = preferencesRepository.dailyTimeLimitMinutes.first()
                val bonus = preferencesRepository.bonusTimeMinutes.first()

                val totalAllowed = dailyLimit + bonus

                if (usedMinutes >= totalAllowed && totalAllowed > 0) {
                    val lockIntent = Intent(applicationContext, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra("SHOW_LOCK_SCREEN", true)
                    }
                    startActivity(lockIntent)
                }
            }
        }
    }

    private fun startBackgroundUpdateCheck() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val updateInfo = updateChecker.checkCloudUpdate("child")
                    if (updateInfo.hasUpdate) {
                        notificationHelper.sendSystemNotification(
                            title = "🚀 Mise à jour disponible (v${updateInfo.latestVersionName})",
                            message = updateInfo.releaseNotes.ifBlank { "Une nouvelle version de l'application est disponible." },
                            notificationType = NotificationHelper.TYPE_APP_UPDATE
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(10 * 60 * 1000L) // Vérification en arrière-plan toutes les 10 minutes
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "time_limit_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
