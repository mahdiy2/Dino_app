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
import com.dino.message.corefeature.presentation.activity.MainActivity

object NotificationHelper {

    private const val CHANNEL_ID = "messages"
    private const val CHANNEL_NAME = "Messages"

    private const val ACTIVITY_CHANNEL_ID = "activity_notifications"
    private const val ACTIVITY_CHANNEL_NAME = "Activity Notifications"

    fun ensureChannel(context: Context) {
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

    fun ensureActivityChannel(context: Context) {
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

    fun showMessageNotification(
        context: Context,
        senderUsername: String,
        body: String
    ) {
        ensureChannel(context)

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

    fun showGroupMessageNotification(
        context: Context,
        groupId: Int,
        groupName: String,
        body: String
    ) {
        ensureChannel(context)

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

    fun showActivityNotification(
        context: Context,
        title: String,
        body: String,
        notificationId: Int = "activity_notif".hashCode()
    ) {
        ensureActivityChannel(context)

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

    fun clearNotificationForUser(context: Context, username: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        manager.cancel(username.hashCode())
    }

    fun clearNotificationForGroup(context: Context, groupId: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager
        manager.cancel("group_$groupId".hashCode())
    }

    fun clearAllNotifications(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        manager.cancelAll()
    }
}
