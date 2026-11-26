package com.example.launchertlphoneenfant.parent.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.launchertlphoneenfant.parent.BuildConfig
import com.example.launchertlphoneenfant.parent.ParentMainActivity
import com.example.launchertlphoneenfant.parent.data.UpdateChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateCheckWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val updateChecker = UpdateChecker()
            val updateInfo = updateChecker.checkCloudUpdate("parent")
            if (updateInfo.hasUpdate && updateInfo.latestVersionCode > BuildConfig.VERSION_CODE) {
                showUpdateSystemNotification(context, updateInfo.latestVersionName, updateInfo.downloadUrl)
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun showUpdateSystemNotification(context: Context, versionName: String, downloadUrl: String) {
        val channelId = "app_updates_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Mises à jour de l'application",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications automatiques de nouvelles versions disponibles"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, ParentMainActivity::class.java).apply {
            action = "ACTION_START_UPDATE"
            putExtra("download_url", downloadUrl)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Mise à jour disponible ! 🎉")
            .setContentText("Appuyez pour installer la version $versionName immédiatement.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(999, notification)
    }
}
