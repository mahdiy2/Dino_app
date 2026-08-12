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
import com.dino.message.corefeature.domain.notification.AppNotificationPresenter
import com.dino.message.corefeature.presentation.activity.MainActivity

class NotificationHelper(
    private val context: Context,
) : AppNotificationPresenter {

    private companion object {
        const val CHANNEL_ID = "messages"
        const val CHANNEL_NAME = "Messages"
        const val ACTIVITY_CHANNEL_ID = "activity_notifications"
        const val ACTIVITY_CHANNEL_NAME = "Activity Notifications"
    }

    override fun ensureMessageChannel() {
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

    override fun ensureActivityChannel() {
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

    override fun showPrivateMessage(
        senderUsername: String,
        body: String
    ) {
        ensureMessageChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("open_chat", senderUsername)
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

    override fun showGroupMessage(
        groupId: Int,
        groupName: String,
        body: String
    ) {
        ensureMessageChannel()

        val deepLink = Uri.parse("secmessage://group/$groupId")
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("open_group_id", groupId)
            putExtra("open_group_name", groupName)
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

    override fun showActivity(
        title: String,
        body: String,
        notificationId: Int
    ) {
        ensureActivityChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("open_activity_notifications", true)
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

    override fun clearPrivateChat(username: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        manager.cancel(username.hashCode())
    }

    override fun clearGroupChat(groupId: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager
        manager.cancel("group_$groupId".hashCode())
    }

    override fun clearAll() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        manager.cancelAll()
    }
}
