package com.dino.message

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Application
import android.app.Activity
import android.os.Bundle
import android.os.Build
import com.dino.message.chatfeature.data.notification.ActiveChatTracker
import com.dino.message.chatfeature.data.notification.ActiveGroupTracker
import com.dino.message.crashfeature.data.service.CrashReporter
import com.dino.message.corefeature.presentation.util.UIEvent
import com.dino.message.di.appModules
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import com.dino.message.chatfeature.data.notification.InAppNotificationManager
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.android.ext.android.inject
import org.koin.core.context.startKoin

class DinoOrderApplication: Application() {

    val crashReporter: CrashReporter by inject()

    /**
     * A mutable shared flow used for emitting UI events.
     */
    val mutableUIEventFlow: MutableSharedFlow<UIEvent> = MutableSharedFlow()

    /**
     * A shared flow representing UI events.
     */
    val uIEventFlow: SharedFlow<UIEvent> = mutableUIEventFlow

    val inAppNotificationManager: InAppNotificationManager by inject()
    private var startedActivitiesCount: Int = 0

    override fun onCreate() {
        super.onCreate()
        if (isCrashUiProcess()) {
            return
        }
        startKoin {
            androidLogger()
            androidContext(this@DinoOrderApplication)
            modules(appModules)
        }
        crashReporter.addBreadcrumb(
            category = "lifecycle",
            message = "Application created version=${BuildConfig.VERSION_NAME} buildType=${BuildConfig.BUILD_TYPE}"
        )
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
            override fun onActivityStarted(activity: Activity) {
                startedActivitiesCount += 1
                crashReporter.addBreadcrumb(
                    category = "lifecycle",
                    message = "Activity started=${activity.simpleScreenName()} activeCount=$startedActivitiesCount"
                )
            }

            override fun onActivityStopped(activity: Activity) {
                if (activity.isChangingConfigurations) return

                startedActivitiesCount = (startedActivitiesCount - 1).coerceAtLeast(0)
                crashReporter.addBreadcrumb(
                    category = "lifecycle",
                    message = "Activity stopped=${activity.simpleScreenName()} activeCount=$startedActivitiesCount"
                )
                if (startedActivitiesCount == 0) {
                    crashReporter.addBreadcrumb(
                        category = "lifecycle",
                        message = "Application moved to background"
                    )
                    ActiveChatTracker.clearActiveChat()
                    ActiveGroupTracker.clearActiveGroup()
                    inAppNotificationManager.disconnect()
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

            override fun onActivityResumed(activity: Activity) {
                crashReporter.addBreadcrumb(
                    category = "navigation",
                    message = "Visible screen=${activity.simpleScreenName()}"
                )
            }

            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    private fun Activity.simpleScreenName(): String {
        val simpleName = javaClass.simpleName
        if (simpleName.isNotBlank()) return simpleName
        return javaClass.name
    }
}
