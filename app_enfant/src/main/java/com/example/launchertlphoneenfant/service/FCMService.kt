package com.example.launchertlphoneenfant.service

import com.example.launchertlphoneenfant.data.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Notification Enfant"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "Vous avez reçu un nouveau message."
        val type = remoteMessage.data["type"] ?: ""

        val notificationHelper = NotificationHelper(applicationContext)
        notificationHelper.sendSystemNotification(
            title = title,
            message = body,
            notificationType = type
        )
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token FCM enregistré
    }
}
