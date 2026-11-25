package com.dino.message.preview

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.navigation.NavDestination.Companion.hasRoute
import com.dino.message.corefeature.data.remote.createAppHttpClient
import com.dino.message.corefeature.presentation.navigation.AppRoute
import com.dino.message.corefeature.presentation.navigation.AuthenticatedApp
import com.dino.message.corefeature.presentation.navigation.BrowserShellSession
import io.ktor.client.engine.js.Js
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.flow.collect
import org.w3c.dom.events.Event

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val http = createAppHttpClient(Js, window.location.origin + "/")
    val session = BrowserShellSession(http)
    ComposeViewport(requireNotNull(document.body)) {
        AuthenticatedApp(session) { navigator, signedIn ->
            val activeSession = rememberUpdatedState(signedIn)
            val initialHash = remember { window.location.hash }
            val accountPushed = remember { mutableStateOf(false) }
            LaunchedEffect(navigator) {
                var firstDestination = true
                navigator.currentBackStackEntryFlow.collect { entry ->
                    val hash = when {
                        entry.destination.hasRoute<AppRoute.Main>() -> "#home"
                        entry.destination.hasRoute<AppRoute.Profile>() -> "#account"
                        entry.destination.hasRoute<AppRoute.Login>() -> "#login"
                        else -> return@collect
                    }
                    if (firstDestination && hash == "#home" && initialHash == "#account") {
                        firstDestination = false
                        navigator.navigate(AppRoute.Profile()) { launchSingleTop = true }
                        return@collect
                    }
                    firstDestination = false
                    if (window.location.hash != hash) {
                        when {
                            hash == "#account" -> {
                                accountPushed.value = true
                                window.history.pushState(null, "", hash)
                            }
                            hash == "#home" && window.location.hash == "#account" && accountPushed.value ->
                                window.history.back()
                            else -> {
                                accountPushed.value = false
                                window.history.replaceState(null, "", hash)
                            }
                        }
                    }
                }
            }
            DisposableEffect(navigator) {
                val onHistoryChange: (Event) -> Unit = {
                    val current = navigator.currentDestination
                    when {
                        current == null || current.hasRoute<AppRoute.Splash>() -> Unit
                        !activeSession.value -> window.history.replaceState(null, "", "#login")
                        window.location.hash == "#account" && !current.hasRoute<AppRoute.Profile>() ->
                            navigator.navigate(AppRoute.Profile()) { launchSingleTop = true }
                        window.location.hash != "#account" && current.hasRoute<AppRoute.Profile>() ->
                            navigator.popBackStack()
                    }
                }
                window.addEventListener("popstate", onHistoryChange)
                onDispose { window.removeEventListener("popstate", onHistoryChange) }
            }
        }
    }
}
