package com.dino.message.authfeature.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import com.dino.message.corefeature.domain.model.AppStringKey
import com.dino.message.authfeature.domain.usecase.LoginUseCase
import com.dino.message.authfeature.domain.usecase.GetKeyStatusUseCase
import com.dino.message.authfeature.domain.usecase.RotatePublicKeyUseCase
import com.dino.message.authfeature.domain.usecase.SyncDeviceIdUseCase
import com.dino.message.corefeature.domain.crypto.MessageCrypto
import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.domain.notification.NotificationSocketConnection
import com.dino.message.corefeature.domain.session.AuthSessionStore
import com.dino.message.corefeature.domain.settings.AppSettingsStore
import com.dino.message.corefeature.presentation.util.PopBackStackLevel
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.corefeature.domain.resource.StringResolver
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.corefeature.presentation.util.UiEventBus
import com.dino.message.corefeature.presentation.viewmodel.ScopedStateHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.dino.message.corefeature.presentation.navigation.AppRoute

class LoginScreenViewModel constructor(
    private val stringResolver: StringResolver,
    uiEventBus: UiEventBus,
    private val authSessionStore: AuthSessionStore,
    private val appSettingsStore: AppSettingsStore,
    private val loginUseCase: LoginUseCase,
    private val getKeyStatusUseCase: GetKeyStatusUseCase,
    private val rotatePublicKeyUseCase: RotatePublicKeyUseCase,
    private val syncDeviceIdUseCase: SyncDeviceIdUseCase,
    private val messageCrypto: MessageCrypto,
    private val notificationSocketConnection: NotificationSocketConnection,
    ) : ScopedStateHolder(),
    HaveUIEvent by HaveUIEventImpl(uiEventBus){

    val userName = mutableStateOf("")
    val password = mutableStateOf("")

    val loginLoadingState = mutableStateOf(false)
    val loginErrorMessage = mutableStateOf<String?>(null)

    val usernameTextFieldIsError = mutableStateOf(false)
    val passwordTextFieldIsError = mutableStateOf(false)
    val currentLanguage: Flow<AppLanguage> = appSettingsStore.getLanguage()

    private fun login(username: String, pass: String) = viewModelScope.launch {
        loginUseCase.execute(
            username, pass
        ).collect { data ->
            when(data){
                is Resource.Success -> {
                    authSessionStore.setUsername(username)
                    authSessionStore.setToken(data.data.accessToken)
                    authSessionStore.setRefreshToken(data.data.refreshToken)
                    ensureLocalKeysThenContinue(data.data.accessToken)
                }
                is Resource.Loading -> {
                    loginLoadingState.value = true

                }
                is Resource.Error -> {
                    loginErrorMessage.value =
                        data.message.resolveWith(stringResolver)
                    usernameTextFieldIsError.value = true
                    passwordTextFieldIsError.value = true
                    loginLoadingState.value = false
                }
            }
        }
    }

    private fun ensureLocalKeysThenContinue(accessToken: String) = viewModelScope.launch {
        if (!isPrivateKeyEmpty() && hasServerPublicKey()) {
            authSessionStore.setUserLogIn(true)
            syncDeviceIdBestEffort()
            loginLoadingState.value = false
            loginErrorMessage.value = null
            notificationSocketConnection.connect(accessToken)
            navigateToMainScreen()
            return@launch
        }

        val keyPair = messageCrypto.generateRsaKeyPair()
        val publicKeyBase64 = keyPair.publicKeyBase64
        val privateKeyBase64 = keyPair.privateKeyBase64

        rotatePublicKeyUseCase.execute(publicKeyBase64).collect { res ->
            when (res) {
                is Resource.Loading -> {
                    loginLoadingState.value = true
                }
                is Resource.Success -> {
                    authSessionStore.savePrivateKey(privateKeyBase64)
                    authSessionStore.savePublicKey(publicKeyBase64)
                    authSessionStore.setUserLogIn(true)
                    syncDeviceIdBestEffort()
                    loginLoadingState.value = false
                    loginErrorMessage.value = null
                    notificationSocketConnection.connect(accessToken)
                    navigateToMainScreen()
                }
                is Resource.Error -> {
                    authSessionStore.setUserLogIn(false)
                    authSessionStore.setToken("")
                    authSessionStore.setRefreshToken("")
                    loginLoadingState.value = false
                    loginErrorMessage.value =
                        stringResolver.resolve(AppStringKey.LoginKeyRecoveryFailed)
                }
            }
        }
    }

    private suspend fun hasServerPublicKey(): Boolean {
        var hasServerKey = false
        getKeyStatusUseCase.execute().collect { result ->
            when (result) {
                is Resource.Success -> {
                    hasServerKey = result.data.hasServerPublicKey
                }
                else -> Unit
            }
        }
        return hasServerKey
    }

    fun usernameOnValueChange(value: String){
        usernameTextFieldIsError.value = false
        passwordTextFieldIsError.value = false
        loginErrorMessage.value = null
        userName.value = value
    }

    fun passwordOnValueChange(value: String){
        usernameTextFieldIsError.value = false
        passwordTextFieldIsError.value = false
        loginErrorMessage.value = null
        password.value = value
    }

    fun onLoginBtnClicked(){
        if (loginLoadingState.value) return
        loginErrorMessage.value = null
        login(userName.value, password.value)
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val nextLanguage = when (appSettingsStore.getLanguage().first()) {
                AppLanguage.Farsi -> AppLanguage.English
                AppLanguage.English -> AppLanguage.Farsi
            }
            appSettingsStore.saveLanguage(nextLanguage)
        }
    }

    fun navigateToRegisterScreen(){
        navigateToDestination(AppRoute.Register, viewModelScope)
    }

    fun navigateToMainScreen(){
        navigateWithPopBackStackToDestination(
            AppRoute.Thread,
            viewModelScope,
            PopBackStackLevel.ALL
        )
    }

    private fun isPrivateKeyEmpty(): Boolean {
        return authSessionStore.getPrivateKey().isNullOrEmpty()
    }

    private fun syncDeviceIdBestEffort() = viewModelScope.launch {
        syncDeviceIdUseCase.execute().collect { _ -> }
    }
}
