package com.dino.message

import android.app.Application
import android.app.Activity
import android.os.Bundle
import com.dino.message.corefeature.data.upload.AndroidMediaOutbox
import com.dino.message.corefeature.data.system.AndroidPlatformInfoProvider
import com.dino.message.crashfeature.data.service.CrashReporter
import com.dino.message.corefeature.domain.config.AppConfig
import com.dino.message.corefeature.domain.lifecycle.AppLifecycleDispatcher
import com.dino.message.corefeature.domain.lifecycle.AppLifecycleEvent
import com.dino.message.chatfeature.data.notification.NotificationHelper
import com.dino.message.corefeature.domain.startup.AppStartupCoordinator
import com.dino.message.corefeature.presentation.util.UiEventBus
import com.dino.message.di.appModules
import com.dino.message.threadfeature.presentation.music.MusicPlaybackManager
import com.dino.message.chatfeature.data.notification.InAppNotificationManager
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.android.ext.android.inject
import org.koin.core.context.startKoin

class DinoOrderApplication: Application() {

    val crashReporter: CrashReporter by inject()

    val uiEventBus: UiEventBus by inject()

    val inAppNotificationManager: InAppNotificationManager by inject()
    private val mediaOutbox: AndroidMediaOutbox by inject()
    private val appConfig: AppConfig by inject()
    private val appLifecycleDispatcher: AppLifecycleDispatcher by inject()
    private val notificationHelper: NotificationHelper by inject()
    private val appStartupCoordinator: AppStartupCoordinator by inject()
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
            message = "Application created version=${appConfig.versionName} buildType=${appConfig.buildType}"
        )
        crashReporter.installGlobalExceptionHandler()
        crashReporter.flushPendingLogsAsync()
        mediaOutbox.restore()
        val commonStartup = appStartupCoordinator
        notificationHelper.ensureAppChannels()
        MusicPlaybackManager.initialize(this)
        commonStartup.initializeCommonState()
        registerAppBackgroundTracker()
    }

    private fun isCrashUiProcess(): Boolean {
        return AndroidPlatformInfoProvider()
            .currentProcessName
            ?.endsWith(":crash_report") == true
    }

    private fun registerAppBackgroundTracker() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                startedActivitiesCount += 1
                val screenName = activity.simpleScreenName()
                crashReporter.addBreadcrumb(
                    category = "lifecycle",
                    message = "Activity started=$screenName activeCount=$startedActivitiesCount"
                )
                appLifecycleDispatcher.dispatch(
                    AppLifecycleEvent.ScreenStarted(
                        screenName = screenName,
                        activeScreenCount = startedActivitiesCount,
                    )
                )
            }

            override fun onActivityStopped(activity: Activity) {
                if (activity.isChangingConfigurations) return

                startedActivitiesCount = (startedActivitiesCount - 1).coerceAtLeast(0)
                val screenName = activity.simpleScreenName()
                crashReporter.addBreadcrumb(
                    category = "lifecycle",
                    message = "Activity stopped=$screenName activeCount=$startedActivitiesCount"
                )
                appLifecycleDispatcher.dispatch(
                    AppLifecycleEvent.ScreenStopped(
                        screenName = screenName,
                        activeScreenCount = startedActivitiesCount,
                    )
                )
                if (startedActivitiesCount == 0) {
                    crashReporter.addBreadcrumb(
                        category = "lifecycle",
                        message = "Application moved to background"
                    )
                    appLifecycleDispatcher.dispatch(AppLifecycleEvent.EnteredBackground)
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

            override fun onActivityResumed(activity: Activity) {
                val screenName = activity.simpleScreenName()
                crashReporter.addBreadcrumb(
                    category = "navigation",
                    message = "Visible screen=$screenName"
                )
                appLifecycleDispatcher.dispatch(AppLifecycleEvent.ScreenVisible(screenName))
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
