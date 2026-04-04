package com.dino.message.chatfeature.data.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.dino.message.R

class BackgroundSocketService : Service() {
    private var notificationManager: InAppNotificationManager? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val token = getToken()
        if (token != null) {
            notificationManager = InAppNotificationManager(applicationContext)
            notificationManager?.connect(token)
            Log.d(TAG, "Background socket service started")
        } else {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        notificationManager?.disconnect()
        notificationManager = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Message Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sec Message")
            .setContentText("Listening for messages")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun getToken(): String? {
        return try {
            val masterKey = MasterKey.Builder(applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val prefs = EncryptedSharedPreferences.create(
                applicationContext,
                "sec_message",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            prefs.getString("TOKEN", null)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "BackgroundSocketService"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "socket_service"

        fun start(context: Context) {
            val intent = Intent(context, BackgroundSocketService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BackgroundSocketService::class.java))
        }
    }
}
