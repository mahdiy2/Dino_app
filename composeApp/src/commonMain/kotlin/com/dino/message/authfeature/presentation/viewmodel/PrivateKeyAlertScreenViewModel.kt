package com.dino.message.authfeature.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import com.dino.message.authfeature.domain.util.RegisterConfirmErrorPolicy
import com.dino.message.authfeature.domain.usecase.ConfirmRegistrationUseCase
import com.dino.message.corefeature.presentation.util.PopBackStackLevel
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.corefeature.domain.resource.StringResolver
import com.dino.message.corefeature.domain.session.AuthSessionStore
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.corefeature.presentation.util.UiEventBus
import com.dino.message.corefeature.presentation.viewmodel.ScopedStateHolder
import kotlinx.coroutines.launch
import com.dino.message.corefeature.presentation.navigation.AppRoute

class PrivateKeyAlertScreenViewModel constructor(
    private val stringResolver: StringResolver,
    uiEventBus: UiEventBus,
    private val confirmRegistrationUseCase: ConfirmRegistrationUseCase,
    private val authSessionStore: AuthSessionStore,
) : ScopedStateHolder(),
    HaveUIEvent by HaveUIEventImpl(uiEventBus) {

    val confirmLoadingState = mutableStateOf(false)
    val confirmErrorMessage = mutableStateOf<String?>(null)

    fun getPendingPrivateKey() = authSessionStore.getPendingPrivateKey()

    fun onContinueClicked(hasCopied: Boolean) {
        if (confirmLoadingState.value) return
        confirmErrorMessage.value = null

        if (!hasCopied) {
            confirmErrorMessage.value = "Please copy your private key first."
            return
        }

        val registrationId = authSessionStore.getPendingRegistrationId()
        if (registrationId.isNullOrBlank()) {
            confirmErrorMessage.value = "Registration session is missing. Please register again."
            navigateToRegisterScreenAndResetPending()
            return
        }

        viewModelScope.launch {
            confirmRegistrationUseCase.execute(registrationId).collect { res ->
                when (res) {
                    is Resource.Loading -> {
                        confirmLoadingState.value = true
                    }

                    is Resource.Success -> {
                        confirmLoadingState.value = false
                        confirmErrorMessage.value = null
                        authSessionStore.finalizePendingRegistration()
                        navigateToLoginScreen()
                    }

                    is Resource.Error -> {
                        confirmLoadingState.value = false
                        val errorMessage =
                            res.message.resolveWith(stringResolver)
                        confirmErrorMessage.value = errorMessage

                        if (RegisterConfirmErrorPolicy.shouldClearPendingSession(errorMessage)) {
                            authSessionStore.clearPendingRegistration()
                        }
                    }
                }
            }
        }
    }

    fun navigateToRegisterScreenAndResetPending() {
        authSessionStore.clearPendingRegistration()
        navigateWithPopBackStackToDestination(
            route = AppRoute.Register,
            scope = viewModelScope,
            level = PopBackStackLevel.ALL
        )
    }

    fun navigateToLoginScreen() {
        navigateWithPopBackStackToDestination(
            route = AppRoute.Login,
            scope = viewModelScope,
            level = PopBackStackLevel.ALL
        )
    }
}
