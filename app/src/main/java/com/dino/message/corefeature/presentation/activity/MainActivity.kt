package com.dino.message.corefeature.presentation.activity

import android.Manifest
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.webkit.WebView
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.navigation.BottomSheetNavigator
import androidx.compose.material.navigation.ModalBottomSheetLayout
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.destinations.ChatScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ActivityNotificationScreenDestination
import com.ramcosta.composedestinations.generated.destinations.GroupChatScreenDestination
import com.dino.message.chatfeature.data.notification.NotificationHelper
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.domain.model.toLayoutDirection
import com.dino.message.corefeature.domain.model.toLocale
import com.dino.message.corefeature.presentation.activity.theme.SecMessageTheme
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.util.HandleUIEvent
import com.dino.message.corefeature.presentation.util.LocaleUtils
import com.dino.message.corefeature.presentation.viewmodel.MainActivityViewModel
import com.dino.message.threadfeature.presentation.music.MusicPlaybackManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.ramcosta.composedestinations.generated.NavGraphs
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val app: Context by inject()

    private val sPrefManager: SPrefManager by inject()

    private val viewModel: MainActivityViewModel by viewModel()

    private var navController: NavController? = null

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        connectNotificationSocket()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        // Determine the system theme based on the current configuration
        val systemTheme =
            when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> true
                Configuration.UI_MODE_NIGHT_NO -> false
                else -> false
            }

        enableEdgeToEdge()
        setContent {

            // Retrieve the app theme and app language from the view model
            val theme = viewModel.getSystemTheme(systemTheme).collectAsState(initial = systemTheme)
            val appLanguage = viewModel.appLanguage.collectAsState(initial = AppLanguage.Farsi)

            val expandedSheetState = rememberModalBottomSheetState(
                initialValue = ModalBottomSheetValue.Hidden,
                skipHalfExpanded = true
            )
            val bottomSheetNavigator = remember(expandedSheetState) {
                BottomSheetNavigator(expandedSheetState)
            }

            // Create a navigation controller
            navController = rememberNavController(bottomSheetNavigator)

            // Set the app's locale based on the selected language
            LocaleUtils.setLocale(LocalContext.current, appLanguage.value.toLocale())

            // Handle UI events and update the app's state
            HandleUIEvent(app as DinoOrderApplication, navController!!)

            SecMessageTheme(
                darkTheme = theme.value,
                appLanguage = appLanguage.value
            ) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides appLanguage.value.toLayoutDirection()
                ) {
                    ModalBottomSheetLayout(
                        bottomSheetNavigator = bottomSheetNavigator,
                        sheetShape = RoundedCornerShape(topEnd = DPExtraLarge, topStart = DPExtraLarge),
                        sheetBackgroundColor = MaterialTheme.colorScheme.surface,
                        sheetContentColor = MaterialTheme.colorScheme.onSurface,
                    ){
                        DestinationsNavHost(
                            navGraph = NavGraphs.root,
//                        startRoute = NavGraphs.root.startRoute, // Add this line
                            navController = navController as NavHostController
                        )
                    }
                    // Set the app's navigation graph and navigation controller


                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationDeepLink(intent)
        handleGroupNotificationDeepLink(intent)
        handleActivityNotificationDeepLink(intent)
    }

    override fun onResume() {
        super.onResume()
        connectNotificationSocket()
        handleNotificationDeepLink(intent)
        handleGroupNotificationDeepLink(intent)
        handleActivityNotificationDeepLink(intent)
    }

    override fun onStop() {
        super.onStop()
        val dinoApp = applicationContext as? DinoOrderApplication
        dinoApp?.inAppNotificationManager?.disconnect()
        if (isFinishing) {
            MusicPlaybackManager.stop(applicationContext)
        }
    }

    override fun onDestroy() {
        val dinoApp = applicationContext as? DinoOrderApplication
        dinoApp?.inAppNotificationManager?.disconnect()
        super.onDestroy()
    }

    private fun connectNotificationSocket() {
        val token = sPrefManager.getToken()
        if (token.isNotBlank()) {
            val dinoApp = applicationContext as DinoOrderApplication
            dinoApp.inAppNotificationManager.connect(token)
        }
    }

    private fun handleNotificationDeepLink(intent: Intent?) {
        val chatUsername = intent?.getStringExtra("open_chat")
        if (chatUsername.isNullOrBlank()) return

        intent.removeExtra("open_chat")

        NotificationHelper.clearNotificationForUser(this, chatUsername)

        lifecycleScope.launch {
            delay(300)
            val nav = navController
            if (nav != null) {
                try {
                    val direction = ChatScreenDestination(
                        chatId = chatUsername,
                        displayName = chatUsername
                    )
                    nav.navigate(direction.route) {
                        launchSingleTop = true
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to navigate to chat: $chatUsername", e)
                }
            }
        }
    }

    private fun handleActivityNotificationDeepLink(intent: Intent?) {
        val openActivity = intent?.getBooleanExtra("open_activity_notifications", false) ?: false
        if (!openActivity) return

        intent?.removeExtra("open_activity_notifications")

        lifecycleScope.launch {
            delay(300)
            val nav = navController
            if (nav != null) {
                try {
                    nav.navigate(ActivityNotificationScreenDestination.route) {
                        launchSingleTop = true
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Failed to navigate to activity notifications", e)
                }
            }
        }
    }

    private fun handleGroupNotificationDeepLink(intent: Intent?) {
        val safeIntent = intent ?: return
        val data = safeIntent.data
        val deepLinkGroupId = data?.lastPathSegment?.toIntOrNull() ?: -1
        val deepLinkGroupName = data?.getQueryParameter("name")

        val groupId = if (deepLinkGroupId > 0) {
            deepLinkGroupId
        } else {
            safeIntent.getIntExtra("open_group_id", -1)
        }
        if (groupId <= 0) return

        val groupName = deepLinkGroupName ?: safeIntent.getStringExtra("open_group_name")
        safeIntent.removeExtra("open_group_id")
        safeIntent.removeExtra("open_group_name")
        NotificationHelper.clearNotificationForGroup(this, groupId)

        val nav = navController
        if (nav != null && safeIntent.data != null) {
            val handledByDeepLink = nav.handleDeepLink(safeIntent)
            if (handledByDeepLink) {
                safeIntent.data = null
                return
            }
        }

        lifecycleScope.launch {
            delay(300)
            val fallbackNav = navController ?: return@launch
            try {
                val direction = GroupChatScreenDestination(
                    groupId = groupId,
                    groupName = groupName
                )
                fallbackNav.navigate(direction.route) {
                    launchSingleTop = true
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to navigate to group: $groupId", e)
            }
        }
    }
}
//@Composable
//fun StartApplication(onWebViewReady: (WebView) -> Unit) {
//    val navController = rememberNavController()
//    val start = "splash_screen"
//
//    NavHost(navController = navController, startDestination = start) {
//        composable("splash_screen") {
//            SplashScreen(navController = navController)
//        }
//        composable("main_page") {
//            MainScreen(navController = navController, onWebViewReady = onWebViewReady)
//        }
//    }
//}
