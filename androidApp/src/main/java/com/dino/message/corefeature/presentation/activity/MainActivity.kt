package com.dino.message.corefeature.presentation.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.presentation.util.toLayoutDirection
import com.dino.message.corefeature.domain.model.toLocale
import com.dino.message.corefeature.domain.notification.AppNotificationController
import com.dino.message.corefeature.domain.notification.NotificationSocketController
import com.dino.message.corefeature.presentation.activity.theme.SecMessageTheme
import com.dino.message.corefeature.presentation.platform.LocalPlatformBackHandler
import com.dino.message.corefeature.presentation.platform.LocalPlatformMediaRenderer
import com.dino.message.corefeature.presentation.platform.LocalPlatformUiEffect
import com.dino.message.corefeature.presentation.platform.rememberAndroidPlatformBackHandler
import com.dino.message.corefeature.presentation.platform.rememberAndroidPlatformMediaRenderer
import com.dino.message.corefeature.presentation.platform.rememberAndroidPlatformUiEffect
import com.dino.message.corefeature.presentation.navigation.AndroidIntentDeepLinkHandler
import com.dino.message.corefeature.presentation.navigation.AppNavGraph
import com.dino.message.corefeature.presentation.navigation.AppRoute
import com.dino.message.corefeature.presentation.navigation.NotificationClearTarget
import com.dino.message.corefeature.presentation.util.HandleUIEvent
import com.dino.message.corefeature.presentation.util.LocaleUtils
import com.dino.message.corefeature.presentation.util.UiEventBus
import com.dino.message.corefeature.presentation.viewmodel.KoinStateHolderFactory
import com.dino.message.corefeature.presentation.viewmodel.LocalStateHolderFactory
import com.dino.message.corefeature.presentation.viewmodel.MainActivityViewModel
import com.dino.message.corefeature.presentation.viewmodel.rememberStateHolder
import com.dino.message.threadfeature.presentation.music.MusicPlaybackManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val appNotificationController: AppNotificationController by inject()

    private val notificationSocketController: NotificationSocketController by inject()

    private val androidIntentDeepLinkHandler: AndroidIntentDeepLinkHandler by inject()

    private val uiEventBus: UiEventBus by inject()

    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        connectNotificationSocket()

        requestPostNotificationsPermissionIfNeeded()

        // Determine the system theme based on the current configuration
        val systemTheme =
            when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> true
                Configuration.UI_MODE_NIGHT_NO -> false
                else -> false
            }

        enableEdgeToEdge()
        setContent {
            val stateHolderFactory = remember { KoinStateHolderFactory() }
            CompositionLocalProvider(LocalStateHolderFactory provides stateHolderFactory) {
                val viewModel: MainActivityViewModel = rememberStateHolder()

                // Retrieve the app theme and app language from the view model
                val theme = viewModel.getSystemTheme(systemTheme).collectAsState(initial = systemTheme)
                val appLanguage = viewModel.appLanguage.collectAsState(initial = AppLanguage.Farsi)

                val platformUiEffect = rememberAndroidPlatformUiEffect()
                val platformBackHandler = rememberAndroidPlatformBackHandler()
                val platformMediaRenderer = rememberAndroidPlatformMediaRenderer()
                val activeModalRoute = remember { mutableStateOf<AppRoute?>(null) }

                // Create a navigation controller
                navController = rememberNavController()

                // Set the app's locale based on the selected language
                LocaleUtils.setLocale(LocalContext.current, appLanguage.value.toLocale())

                // Handle UI events and update the app's state
                HandleUIEvent(uiEventBus, navController!!, activeModalRoute)

                SecMessageTheme(
                    darkTheme = theme.value,
                    appLanguage = appLanguage.value
                ) {
                    CompositionLocalProvider(
                        LocalPlatformUiEffect provides platformUiEffect,
                        LocalPlatformBackHandler provides platformBackHandler,
                        LocalPlatformMediaRenderer provides platformMediaRenderer,
                        LocalLayoutDirection provides appLanguage.value.toLayoutDirection()
                    ) {
                        AppNavGraph(navController = navController!!, activeModalRoute = activeModalRoute)
                    }
                }
            }
        }
        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    override fun onResume() {
        super.onResume()
        connectNotificationSocket()
    }

    override fun onStop() {
        super.onStop()
        notificationSocketController.disconnect()
        if (isFinishing) {
            MusicPlaybackManager.stop()
        }
    }

    override fun onDestroy() {
        notificationSocketController.disconnect()
        super.onDestroy()
    }

    private fun connectNotificationSocket() {
        notificationSocketController.connectIfAuthenticated()
    }

    private fun requestPostNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return

        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), POST_NOTIFICATIONS_REQUEST_CODE)
    }

    private fun handleDeepLink(intent: Intent?) {
        val command = androidIntentDeepLinkHandler.consume(intent) ?: return
        when (val target = command.notificationClearTarget) {
            is NotificationClearTarget.PrivateChat -> appNotificationController.clearPrivateChat(target.username)
            is NotificationClearTarget.GroupChat -> appNotificationController.clearGroupChat(target.groupId)
            null -> Unit
        }

        lifecycleScope.launch {
            delay(300)
            val nav = navController ?: return@launch
            try {
                nav.navigate(command.route) {
                    launchSingleTop = true
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to navigate deep link: ${command.route}", e)
            }
        }
    }

    companion object {
        private const val POST_NOTIFICATIONS_REQUEST_CODE = 1001
    }
}
