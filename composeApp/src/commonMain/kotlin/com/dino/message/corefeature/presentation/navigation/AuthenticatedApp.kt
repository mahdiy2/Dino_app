package com.dino.message.corefeature.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.dino.message.authfeature.presentation.screen.LoginContent
import com.dino.message.authfeature.domain.model.AccountKeyRecord
import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.presentation.util.PopBackStackLevel
import com.dino.message.corefeature.presentation.util.UIEvent
import com.dino.message.corefeature.presentation.util.UiEventBus
import com.dino.message.corefeature.presentation.viewmodel.LocalStateHolderFactory
import com.dino.message.corefeature.presentation.viewmodel.ScopedStateHolder
import com.dino.message.corefeature.presentation.viewmodel.StateHolderFactory
import com.dino.message.corefeature.presentation.viewmodel.rememberStateHolder
import kotlin.reflect.KClass
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

@Composable
internal fun AuthenticatedApp(
    session: ShellSession,
    keys: AccountKeyWorkflow,
    bindNavigation: @Composable (androidx.navigation.NavHostController, Boolean) -> Unit = { _, _ -> },
) {
    val navigator = rememberNavController()
    val events = remember { UiEventBus() }
    val factory = remember(session) { SessionHolderFactory(session, events) }

    MaterialTheme {
        Surface(Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalStateHolderFactory provides factory) {
                val holder = rememberStateHolder(SessionHolder::class)
                LaunchedEffect(holder) { holder.restore() }
                AppShell(navigator, events, factory) { route -> SessionContent(route, holder, keys) }
                bindNavigation(navigator, holder.account != null)
            }
        }
    }
}

private class SessionHolderFactory(
    private val session: ShellSession,
    private val events: UiEventBus,
) : StateHolderFactory {
    override fun <T : Any> create(holderClass: KClass<T>, parameters: List<Any?>): T {
        require(holderClass == SessionHolder::class)
        @Suppress("UNCHECKED_CAST")
        return SessionHolder(session, events) as T
    }
}

private class SessionHolder(
    private val session: ShellSession,
    private val events: UiEventBus,
) : ScopedStateHolder() {
    var account by mutableStateOf<String?>(null)
        private set
    var messagingKey by mutableStateOf<AccountKeyRecord?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun restore() = update { session.restore() }
    fun signIn(username: String, password: String) = update { session.signIn(username, password) }
    fun signOut() = update {
        session.signOut()
        null
    }

    fun updateMessagingKey(key: AccountKeyRecord?) {
        messagingKey = key
    }

    fun openRegistration() {
        scope.launch { events.mutableUIEventFlow.emit(UIEvent.Navigate(AppRoute.Register)) }
    }

    fun openAccount() {
        scope.launch { events.mutableUIEventFlow.emit(UIEvent.Navigate(AppRoute.Profile())) }
    }

    fun goBack() {
        scope.launch { events.mutableUIEventFlow.emit(UIEvent.NavigateUp) }
    }

    private fun update(operation: suspend () -> String?) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                val nextAccount = operation()
                if (account != nextAccount) messagingKey = null
                account = nextAccount
                events.mutableUIEventFlow.subscriptionCount.first { it > 0 }
                events.mutableUIEventFlow.emit(
                    UIEvent.NavigateWithPopBackStack(
                        if (account == null) AppRoute.Login else AppRoute.Main,
                        PopBackStackLevel.ALL,
                    )
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = "The request failed. Check your credentials or backend connection and try again."
            } finally {
                busy = false
            }
        }
    }
}

@Composable
private fun SessionContent(route: AppRoute, holder: SessionHolder, keys: AccountKeyWorkflow) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val account = holder.account
    if (account == null && route == AppRoute.Register) {
        RegistrationControls(keys, holder::goBack, holder::signIn)
        return
    }
    if (account == null && route != AppRoute.Splash) {
        LoginContent(
            username = username,
            password = password,
            usernameIsError = false,
            passwordIsError = holder.error != null,
            loginErrorMessage = holder.error,
            loginLoading = holder.busy,
            currentLanguage = AppLanguage.English,
            onUsernameChange = { username = it },
            onPasswordChange = { password = it },
            onLoginClick = {
                if (username.isNotBlank() && password.isNotEmpty() && !holder.busy) {
                    holder.signIn(username.trim(), password)
                    password = ""
                }
            },
            onCreateAccountClick = holder::openRegistration,
            onToggleLanguage = {},
            showLanguageToggle = false,
        )
        return
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text("SecMessage", style = MaterialTheme.typography.headlineLarge)
        holder.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (route == AppRoute.Splash) {
            if (holder.error == null) Text("Checking session…")
            else Button(onClick = holder::restore, enabled = !holder.busy) { Text("Retry") }
        } else if (account != null) {
            Text("Signed in as @$account")
            if (route is AppRoute.Profile) {
                androidx.compose.runtime.key(account) { AccountKeyControls(keys, holder.messagingKey, holder::updateMessagingKey, holder::restore) }
                Button(onClick = holder::goBack) { Text("Back") }
            } else {
                Button(onClick = holder::openAccount) { Text("Account") }
            }
            Button(onClick = holder::signOut, enabled = !holder.busy) { Text("Sign out") }
        }
    }
}
