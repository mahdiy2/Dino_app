package com.dino.message.chatfeature.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.dino.message.R
import com.dino.message.corefeature.domain.notification.NotificationPayload
import com.dino.message.corefeature.presentation.activity.MainActivity
import com.dino.message.corefeature.presentation.navigation.AndroidIntentDeepLinkHandler.Companion.EXTRA_OPEN_ACTIVITY_NOTIFICATIONS
import com.dino.message.corefeature.presentation.navigation.AndroidIntentDeepLinkHandler.Companion.EXTRA_OPEN_CHAT
import com.dino.message.corefeature.presentation.navigation.AndroidIntentDeepLinkHandler.Companion.EXTRA_OPEN_GROUP_ID
import com.dino.message.corefeature.presentation.navigation.AndroidIntentDeepLinkHandler.Companion.EXTRA_OPEN_GROUP_NAME

class NotificationHelper(
    private val context: Context,
) {

    private companion object {
        const val CHANNEL_ID = "messages"
        const val CHANNEL_NAME = "Messages"
        const val ACTIVITY_CHANNEL_ID = "activity_notifications"
        const val ACTIVITY_CHANNEL_NAME = "Activity Notifications"
    }

    fun ensureAppChannels() {
        ensureMessageChannel()
        ensureActivityChannel()
    }

    fun show(payload: NotificationPayload) {
        when (payload) {
            is NotificationPayload.PrivateMessage -> showPrivateMessage(payload.senderUsername, payload.body)
            is NotificationPayload.GroupMessage -> showGroupMessage(payload.groupId, payload.groupName, payload.body)
            is NotificationPayload.Activity -> showActivity(
                payload.title,
                payload.body,
                payload.eventId?.toInt() ?: "activity_notif".hashCode(),
            )
        }
    }

    private fun ensureMessageChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun ensureActivityChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ACTIVITY_CHANNEL_ID, ACTIVITY_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun showPrivateMessage(
        senderUsername: String,
        body: String
    ) {
        ensureMessageChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_OPEN_CHAT, senderUsername)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            senderUsername.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_message_filled)
            .setContentTitle(senderUsername)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        manager.notify(senderUsername.hashCode(), notification)
    }

    private fun showGroupMessage(
        groupId: Int,
        groupName: String,
        body: String
    ) {
        ensureMessageChannel()

        val deepLink = Uri.parse("secmessage://group/$groupId")
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_OPEN_GROUP_ID, groupId)
            putExtra(EXTRA_OPEN_GROUP_NAME, groupName)
            data = deepLink
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val notificationId = "group_$groupId".hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_message_filled)
            .setContentTitle(groupName)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager
        manager.notify(notificationId, notification)
    }

    private fun showActivity(
        title: String,
        body: String,
        notificationId: Int
    ) {
        ensureActivityChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_OPEN_ACTIVITY_NOTIFICATIONS, true)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ACTIVITY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        manager.notify(notificationId, notification)
    }

    fun clearPrivateChat(username: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        manager.cancel(username.hashCode())
    }

    fun clearGroupChat(groupId: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager
        manager.cancel("group_$groupId".hashCode())
    }

    fun clearAll() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        manager.cancelAll()
    }
}
