package com.dino.message.authfeature.presentation.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.authfeature.domain.util.RegisterConfirmErrorPolicy
import com.dino.message.authfeature.domain.usecase.ConfirmRegistrationUseCase
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.presentation.util.PopBackStackLevel
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import kotlinx.coroutines.launch
import com.dino.message.corefeature.presentation.navigation.AppRoute

class PrivateKeyAlertScreenViewModel constructor(
    app: Context,
    private val confirmRegistrationUseCase: ConfirmRegistrationUseCase,
    private val sPrefManager: SPrefManager,
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    val confirmLoadingState = mutableStateOf(false)
    val confirmErrorMessage = mutableStateOf<String?>(null)

    fun getPendingPrivateKey() = sPrefManager.getPendingPrivateKey()

    fun onContinueClicked(hasCopied: Boolean) {
        if (confirmLoadingState.value) return
        confirmErrorMessage.value = null

        if (!hasCopied) {
            confirmErrorMessage.value = "Please copy your private key first."
            return
        }

        val registrationId = sPrefManager.getPendingRegistrationId()
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
                        sPrefManager.finalizePendingRegistration()
                        navigateToLoginScreen()
                    }

                    is Resource.Error -> {
                        confirmLoadingState.value = false
                        val errorMessage =
                            res.message.asString(getApplication<DinoOrderApplication>())
                        confirmErrorMessage.value = errorMessage

                        if (RegisterConfirmErrorPolicy.shouldClearPendingSession(errorMessage)) {
                            sPrefManager.clearPendingRegistration()
                        }
                    }
                }
            }
        }
    }

    fun navigateToRegisterScreenAndResetPending() {
        sPrefManager.clearPendingRegistration()
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
