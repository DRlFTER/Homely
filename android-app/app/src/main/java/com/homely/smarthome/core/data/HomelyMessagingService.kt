package com.homely.smarthome.core.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.homely.smarthome.MainActivity
import com.homely.smarthome.R
import java.security.MessageDigest

class HomelyMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Safety alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Automatic safety cutoffs and device errors"
            },
        )
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(message.notification?.title ?: "Homely safety alert")
            .setContentText(message.notification?.body ?: "A device needs your attention.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        manager.notify(message.messageId?.hashCode() ?: 1, notification)
    }

    override fun onNewToken(token: String) {
        val user = Firebase.auth.currentUser ?: return
        val tokenId = MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray())
            .take(12)
            .joinToString("") { byte -> "%02x".format(byte) }
        Firebase.firestore.collection("homes").document("demo-home")
            .collection("fcmTokens").document(tokenId)
            .set(
                mapOf(
                    "token" to token,
                    "userId" to user.uid,
                    "platform" to "android",
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
    }

    private companion object {
        const val CHANNEL_ID = "homely_safety_alerts"
    }
}
