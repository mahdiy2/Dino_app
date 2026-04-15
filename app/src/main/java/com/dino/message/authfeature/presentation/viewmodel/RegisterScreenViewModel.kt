package com.dino.message.authfeature.presentation.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.authfeature.domain.usecase.RegisterUseCase
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.presentation.util.RSAUtil
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.ramcosta.composedestinations.generated.destinations.LoginScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PrivateKeyAlertScreenDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class RegisterScreenViewModel @Inject constructor(
    @ApplicationContext app: Context,
    private val registerUseCase: RegisterUseCase,
    private val sPrefManager: SPrefManager,
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    val userName = mutableStateOf("")
    val password = mutableStateOf("")

    val registerLoadingState = mutableStateOf(false)
    val registerErrorMessage = mutableStateOf<String?>(null)

    private val usernameAllowedPattern = Regex("^[A-Za-z0-9._-]+$")

    init {
        if (sPrefManager.hasPendingRegistration()) {
            navigateToPrivateKeyAlert()
        }
    }

    private fun register(
        username: String,
        pass: String,
        publicKey: String,
        clientNonce: String
    ) = viewModelScope.launch {
        registerUseCase.execute(
            username,
            pass,
            publicKey,
            clientNonce
        ).collect { res ->
            when (res) {
                is Resource.Success -> {
                    registerLoadingState.value = false
                    registerErrorMessage.value = null
                    sPrefManager.savePendingRegistrationId(res.data.registrationId)
                    val expiresAt =
                        System.currentTimeMillis() + (res.data.expiresInSeconds * 1000L)
                    sPrefManager.savePendingRegistrationExpiresAt(expiresAt)
                    navigateToPrivateKeyAlert()
                }
                is Resource.Loading -> {
                    registerLoadingState.value = true
                }
                is Resource.Error -> {
                    registerLoadingState.value = false
                    registerErrorMessage.value =
                        res.message.asString(getApplication<DinoOrderApplication>())
                }
            }
        }
    }


    fun onClickBtnRegister() {
        if (registerLoadingState.value) return
        registerErrorMessage.value = null

        val trimmedUsername = userName.value.trim()
        val rawPassword = password.value
        if (trimmedUsername.length < 7) {
            registerErrorMessage.value = "Username must be at least 7 characters."
            return
        }
        if (!isUsernameFormatValid(trimmedUsername)) {
            registerErrorMessage.value =
                "Username can only contain English letters, numbers, '.', '-', and '_'."
            return
        }
        if (rawPassword.length < 10) {
            registerErrorMessage.value = "Password must be at least 10 characters."
            return
        }

        val keyPair = RSAUtil.generateKeyPair()
        val publicKeyBase64 = RSAUtil.getPublicKeyBase64(keyPair.public)
        val privateKeyBase64 = RSAUtil.getPrivateKeyBase64(keyPair.private)
        val clientNonce = UUID.randomUUID().toString()

        sPrefManager.clearPendingRegistration()
        sPrefManager.savePendingPrivateKey(privateKeyBase64)
        sPrefManager.savePendingPublicKey(publicKeyBase64)
        sPrefManager.savePendingRegistrationUsername(trimmedUsername)
        sPrefManager.savePendingRegistrationNonce(clientNonce)

        register(
            trimmedUsername,
            rawPassword,
            publicKeyBase64,
            clientNonce
        )
    }

    fun onUsernameValueChange(value: String) {
        userName.value = value
        registerErrorMessage.value = null
    }

    fun onPasswordValueChange(value: String) {
        password.value = value
        registerErrorMessage.value = null
    }

    fun isUsernameFormatValid(username: String): Boolean {
        return usernameAllowedPattern.matches(username)
    }

    fun navigateToLoginScreen() {
        if (sPrefManager.hasPendingRegistration()) {
            registerErrorMessage.value =
                "You must finish private key backup before going to login."
            navigateToPrivateKeyAlert()
            return
        }
        navigateToDestination(LoginScreenDestination, viewModelScope)
    }

    fun navigateToPrivateKeyAlert() {
        navigateToDestination(PrivateKeyAlertScreenDestination, viewModelScope)
    }
}
