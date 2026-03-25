package com.dino.message

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Application
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import com.dino.message.corefeature.presentation.util.UIEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import com.dino.message.chatfeature.data.notification.InAppNotificationManager

@HiltAndroidApp
class DinoOrderApplication: Application() {

    /**
     * A mutable shared flow used for emitting UI events.
     */
    val mutableUIEventFlow: MutableSharedFlow<UIEvent> = MutableSharedFlow()

    /**
     * A shared flow representing UI events.
     */
    val uIEventFlow: SharedFlow<UIEvent> = mutableUIEventFlow

    val inAppNotificationManager: InAppNotificationManager by lazy { InAppNotificationManager(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "messages", "Messages",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
