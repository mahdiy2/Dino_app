package com.dino.message.authfeature.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import com.dino.message.corefeature.domain.model.AppStringKey
import com.dino.message.authfeature.domain.util.RegisterConfirmErrorPolicy
import com.dino.message.authfeature.domain.usecase.ConfirmRegistrationUseCase
import com.dino.message.authfeature.domain.usecase.RegisterUseCase
import com.dino.message.corefeature.domain.config.AppConfig
import com.dino.message.corefeature.domain.crypto.MessageCrypto
import com.dino.message.corefeature.domain.device.DeviceInfoProvider
import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.domain.resource.StringResolver
import com.dino.message.corefeature.domain.session.AuthSessionStore
import com.dino.message.corefeature.domain.settings.AppSettingsStore
import com.dino.message.corefeature.domain.system.AppClock
import com.dino.message.corefeature.data.remote.util.CommonUuid
import com.dino.message.corefeature.presentation.util.PopBackStackLevel
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.corefeature.presentation.util.UiEventBus
import com.dino.message.corefeature.presentation.viewmodel.ScopedStateHolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.dino.message.corefeature.presentation.navigation.AppRoute

class RegisterScreenViewModel constructor(
    uiEventBus: UiEventBus,
    private val registerUseCase: RegisterUseCase,
    private val confirmRegistrationUseCase: ConfirmRegistrationUseCase,
    private val authSessionStore: AuthSessionStore,
    private val appSettingsStore: AppSettingsStore,
    private val deviceIdProvider: DeviceInfoProvider,
    private val appConfig: AppConfig,
    private val stringResolver: StringResolver,
    private val messageCrypto: MessageCrypto,
    private val appClock: AppClock,
) : ScopedStateHolder(),
    HaveUIEvent by HaveUIEventImpl(uiEventBus) {

    val userName = mutableStateOf("")
    val password = mutableStateOf("")
    val referralCode = mutableStateOf("")

    val registerLoadingState = mutableStateOf(false)
    val registerErrorMessage = mutableStateOf<String?>(null)
    val currentLanguage: Flow<AppLanguage> = appSettingsStore.getLanguage()

    private val usernameAllowedPattern = Regex("^[A-Za-z0-9._-]+$")
    private val passwordLetterPattern = Regex("[A-Za-z]")
    private val passwordDigitPattern = Regex("\\d")
    private val passwordSymbolPattern = Regex("[^A-Za-z0-9]")

    init {
        if (authSessionStore.hasPendingRegistration()) {
            val registrationId = authSessionStore.getPendingRegistrationId()
            if (registrationId.isNullOrBlank()) {
                authSessionStore.clearPendingRegistration()
            } else {
                confirmPendingRegistration(registrationId)
            }
        }
    }

    private fun register(
        username: String,
        pass: String,
        publicKey: String,
        clientNonce: String,
        referralCode: String?,
        deviceId: String
    ) = viewModelScope.launch {
        registerUseCase.execute(
            username,
            pass,
            publicKey,
            clientNonce,
            referralCode,
            deviceId
        ).collect { res ->
            when (res) {
                is Resource.Success -> {
                    registerErrorMessage.value = null
                    authSessionStore.savePendingRegistrationId(res.data.registrationId)
                    val expiresAt =
                        appClock.currentTimeMillis() + (res.data.expiresInSeconds * 1000L)
                    authSessionStore.savePendingRegistrationExpiresAt(expiresAt)
                    confirmPendingRegistration(res.data.registrationId)
                }
                is Resource.Loading -> {
                    registerLoadingState.value = true
                }
                is Resource.Error -> {
                    registerLoadingState.value = false
                    registerErrorMessage.value = res.message.resolveWith(stringResolver)
                }
            }
        }
    }

    private fun confirmPendingRegistration(registrationId: String) = viewModelScope.launch {
        confirmRegistrationUseCase.execute(registrationId).collect { res ->
            when (res) {
                is Resource.Loading -> {
                    registerLoadingState.value = true
                }
                is Resource.Success -> {
                    registerLoadingState.value = false
                    registerErrorMessage.value = null
                    authSessionStore.finalizePendingRegistration()
                    navigateToLoginAfterRegistration()
                }
                is Resource.Error -> {
                    registerLoadingState.value = false
                    val errorMessage = res.message.resolveWith(stringResolver)
                    registerErrorMessage.value = errorMessage
                    if (RegisterConfirmErrorPolicy.shouldClearPendingSession(errorMessage)) {
                        authSessionStore.clearPendingRegistration()
                    }
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
            registerErrorMessage.value = stringResolver.resolve(AppStringKey.ErrorUsernameMinLength)
            return
        }
        if (!isUsernameFormatValid(trimmedUsername)) {
            registerErrorMessage.value = stringResolver.resolve(AppStringKey.ErrorUsernameAllowedChars)
            return
        }
        if (rawPassword.length < 10) {
            registerErrorMessage.value = stringResolver.resolve(AppStringKey.ErrorPasswordMinLength)
            return
        }
        if (!passwordLetterPattern.containsMatchIn(rawPassword)) {
            registerErrorMessage.value = stringResolver.resolve(AppStringKey.ErrorPasswordRequireLetter)
            return
        }
        if (!passwordDigitPattern.containsMatchIn(rawPassword)) {
            registerErrorMessage.value = stringResolver.resolve(AppStringKey.ErrorPasswordRequireNumber)
            return
        }
        if (appConfig.authPasswordRequireSymbol && !passwordSymbolPattern.containsMatchIn(rawPassword)) {
            registerErrorMessage.value = stringResolver.resolve(AppStringKey.ErrorPasswordRequireSymbol)
            return
        }

        scope.launch {
            val keyPair = messageCrypto.generateRsaKeyPair()
            val publicKeyBase64 = keyPair.publicKeyBase64
            val privateKeyBase64 = keyPair.privateKeyBase64
            val clientNonce = CommonUuid.randomUuidString()
            val normalizedReferralCode = referralCode.value.trim().uppercase().ifBlank { null }
            val deviceId = deviceIdProvider.getDeviceId()

            authSessionStore.clearPendingRegistration()
            authSessionStore.savePendingPrivateKey(privateKeyBase64)
            authSessionStore.savePendingPublicKey(publicKeyBase64)
            authSessionStore.savePendingRegistrationUsername(trimmedUsername)
            authSessionStore.savePendingRegistrationNonce(clientNonce)

            register(
                trimmedUsername,
                rawPassword,
                publicKeyBase64,
                clientNonce,
                normalizedReferralCode,
                deviceId
            )
        }
    }

    fun onUsernameValueChange(value: String) {
        userName.value = value
        registerErrorMessage.value = null
    }

    fun onPasswordValueChange(value: String) {
        password.value = value
        registerErrorMessage.value = null
    }

    fun onReferralCodeValueChange(value: String) {
        referralCode.value = value
        registerErrorMessage.value = null
    }

    fun isUsernameFormatValid(username: String): Boolean {
        return usernameAllowedPattern.matches(username)
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

    fun navigateToLoginScreen() {
        navigateToDestination(AppRoute.Login, viewModelScope)
    }

    private fun navigateToLoginAfterRegistration() {
        navigateWithPopBackStackToDestination(
            route = AppRoute.Login,
            scope = viewModelScope,
            level = PopBackStackLevel.ALL
        )
    }
}
