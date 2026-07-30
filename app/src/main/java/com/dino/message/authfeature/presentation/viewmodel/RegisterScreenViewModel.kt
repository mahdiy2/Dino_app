package com.dino.message.authfeature.presentation.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.BuildConfig
import com.dino.message.R
import com.dino.message.DinoOrderApplication
import com.dino.message.authfeature.domain.util.RegisterConfirmErrorPolicy
import com.dino.message.authfeature.domain.usecase.ConfirmRegistrationUseCase
import com.dino.message.authfeature.domain.usecase.RegisterUseCase
import com.dino.message.corefeature.data.spref.DataStoreUtil
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.data.util.DeviceIdProvider
import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.presentation.util.PopBackStackLevel
import com.dino.message.corefeature.presentation.util.RSAUtil
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.ramcosta.composedestinations.generated.destinations.LoginScreenDestination
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class RegisterScreenViewModel constructor(
    app: Context,
    private val registerUseCase: RegisterUseCase,
    private val confirmRegistrationUseCase: ConfirmRegistrationUseCase,
    private val sPrefManager: SPrefManager,
    private val dataStoreUtil: DataStoreUtil,
    private val deviceIdProvider: DeviceIdProvider,
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    val userName = mutableStateOf("")
    val password = mutableStateOf("")
    val referralCode = mutableStateOf("")

    val registerLoadingState = mutableStateOf(false)
    val registerErrorMessage = mutableStateOf<String?>(null)
    val currentLanguage: Flow<AppLanguage> = dataStoreUtil.getLanguage()

    private val usernameAllowedPattern = Regex("^[A-Za-z0-9._-]+$")
    private val passwordLetterPattern = Regex("[A-Za-z]")
    private val passwordDigitPattern = Regex("\\d")
    private val passwordSymbolPattern = Regex("[^A-Za-z0-9]")

    init {
        if (sPrefManager.hasPendingRegistration()) {
            val registrationId = sPrefManager.getPendingRegistrationId()
            if (registrationId.isNullOrBlank()) {
                sPrefManager.clearPendingRegistration()
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
                    sPrefManager.savePendingRegistrationId(res.data.registrationId)
                    val expiresAt =
                        System.currentTimeMillis() + (res.data.expiresInSeconds * 1000L)
                    sPrefManager.savePendingRegistrationExpiresAt(expiresAt)
                    confirmPendingRegistration(res.data.registrationId)
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

    private fun confirmPendingRegistration(registrationId: String) = viewModelScope.launch {
        confirmRegistrationUseCase.execute(registrationId).collect { res ->
            when (res) {
                is Resource.Loading -> {
                    registerLoadingState.value = true
                }
                is Resource.Success -> {
                    registerLoadingState.value = false
                    registerErrorMessage.value = null
                    sPrefManager.finalizePendingRegistration()
                    navigateToLoginAfterRegistration()
                }
                is Resource.Error -> {
                    registerLoadingState.value = false
                    val errorMessage =
                        res.message.asString(getApplication<DinoOrderApplication>())
                    registerErrorMessage.value = errorMessage
                    if (RegisterConfirmErrorPolicy.shouldClearPendingSession(errorMessage)) {
                        sPrefManager.clearPendingRegistration()
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
            registerErrorMessage.value =
                getApplication<DinoOrderApplication>().getString(R.string.error_username_min_length)
            return
        }
        if (!isUsernameFormatValid(trimmedUsername)) {
            registerErrorMessage.value =
                getApplication<DinoOrderApplication>().getString(R.string.error_username_allowed_chars)
            return
        }
        if (rawPassword.length < 10) {
            registerErrorMessage.value =
                getApplication<DinoOrderApplication>().getString(R.string.error_password_min_length)
            return
        }
        if (!passwordLetterPattern.containsMatchIn(rawPassword)) {
            registerErrorMessage.value =
                getApplication<DinoOrderApplication>().getString(R.string.error_password_require_letter)
            return
        }
        if (!passwordDigitPattern.containsMatchIn(rawPassword)) {
            registerErrorMessage.value =
                getApplication<DinoOrderApplication>().getString(R.string.error_password_require_number)
            return
        }
        if (BuildConfig.AUTH_PASSWORD_REQUIRE_SYMBOL && !passwordSymbolPattern.containsMatchIn(rawPassword)) {
            registerErrorMessage.value =
                getApplication<DinoOrderApplication>().getString(R.string.error_password_require_symbol)
            return
        }

        val keyPair = RSAUtil.generateKeyPair()
        val publicKeyBase64 = RSAUtil.getPublicKeyBase64(keyPair.public)
        val privateKeyBase64 = RSAUtil.getPrivateKeyBase64(keyPair.private)
        val clientNonce = UUID.randomUUID().toString()
        val normalizedReferralCode = referralCode.value.trim().uppercase().ifBlank { null }
        val deviceId = deviceIdProvider.getDeviceId()

        sPrefManager.clearPendingRegistration()
        sPrefManager.savePendingPrivateKey(privateKeyBase64)
        sPrefManager.savePendingPublicKey(publicKeyBase64)
        sPrefManager.savePendingRegistrationUsername(trimmedUsername)
        sPrefManager.savePendingRegistrationNonce(clientNonce)

        register(
            trimmedUsername,
            rawPassword,
            publicKeyBase64,
            clientNonce,
            normalizedReferralCode,
            deviceId
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

    fun onReferralCodeValueChange(value: String) {
        referralCode.value = value
        registerErrorMessage.value = null
    }

    fun isUsernameFormatValid(username: String): Boolean {
        return usernameAllowedPattern.matches(username)
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val nextLanguage = when (dataStoreUtil.getLanguage().first()) {
                AppLanguage.Farsi -> AppLanguage.English
                AppLanguage.English -> AppLanguage.Farsi
            }
            dataStoreUtil.saveLanguage(nextLanguage)
        }
    }

    fun navigateToLoginScreen() {
        navigateToDestination(LoginScreenDestination, viewModelScope)
    }

    private fun navigateToLoginAfterRegistration() {
        navigateWithPopBackStackToDestination(
            direction = LoginScreenDestination,
            scope = viewModelScope,
            level = PopBackStackLevel.ALL
        )
    }
}
