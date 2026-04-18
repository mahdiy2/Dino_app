package com.dino.message

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Application
import android.os.Bundle
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import com.dino.message.chatfeature.data.notification.ActiveChatTracker
import com.dino.message.chatfeature.data.notification.ActiveGroupTracker
import com.dino.message.crashfeature.data.service.CrashReporter
import com.dino.message.corefeature.presentation.util.UIEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import com.dino.message.chatfeature.data.notification.InAppNotificationManager
import javax.inject.Inject

@HiltAndroidApp
class DinoOrderApplication: Application() {

    @Inject
    lateinit var crashReporter: CrashReporter

    /**
     * A mutable shared flow used for emitting UI events.
     */
    val mutableUIEventFlow: MutableSharedFlow<UIEvent> = MutableSharedFlow()

    /**
     * A shared flow representing UI events.
     */
    val uIEventFlow: SharedFlow<UIEvent> = mutableUIEventFlow

    val inAppNotificationManager: InAppNotificationManager by lazy { InAppNotificationManager(this) }
    private var startedActivitiesCount: Int = 0

    override fun onCreate() {
        super.onCreate()
        if (isCrashUiProcess()) {
            return
        }
        crashReporter.installGlobalExceptionHandler()
        crashReporter.flushPendingLogsAsync()
        createNotificationChannel()
        registerAppBackgroundTracker()
    }

    private fun isCrashUiProcess(): Boolean {
        val processName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Application.getProcessName()
        } else {
            null
        }
        return processName?.endsWith(":crash_report") == true
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "messages", "Messages",
                NotificationManager.IMPORTANCE_HIGH
            )
            val activityChannel = NotificationChannel(
                "activity_notifications", "Activity Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            manager.createNotificationChannel(activityChannel)
        }
    }

    private fun registerAppBackgroundTracker() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: android.app.Activity) {
                startedActivitiesCount += 1
            }

            override fun onActivityStopped(activity: android.app.Activity) {
                if (activity.isChangingConfigurations) return

                startedActivitiesCount = (startedActivitiesCount - 1).coerceAtLeast(0)
                if (startedActivitiesCount == 0) {
                    ActiveChatTracker.clearActiveChat()
                    ActiveGroupTracker.clearActiveGroup()
                    inAppNotificationManager.disconnect()
                }
            }

            override fun onActivityCreated(activity: android.app.Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: android.app.Activity) = Unit
            override fun onActivityPaused(activity: android.app.Activity) = Unit
            override fun onActivitySaveInstanceState(activity: android.app.Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: android.app.Activity) = Unit
        })
    }
}
