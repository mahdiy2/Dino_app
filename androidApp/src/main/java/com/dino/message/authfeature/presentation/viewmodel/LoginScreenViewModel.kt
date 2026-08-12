package com.dino.message.authfeature.presentation.viewmodel

import com.dino.message.corefeature.presentation.util.asString
import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.R
import com.dino.message.DinoOrderApplication
import com.dino.message.authfeature.domain.usecase.LoginUseCase
import com.dino.message.authfeature.domain.usecase.GetKeyStatusUseCase
import com.dino.message.authfeature.domain.usecase.RotatePublicKeyUseCase
import com.dino.message.authfeature.domain.usecase.SyncDeviceIdUseCase
import com.dino.message.corefeature.data.spref.DataStoreUtil
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.domain.crypto.MessageCrypto
import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.presentation.util.PopBackStackLevel
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.dino.message.corefeature.presentation.navigation.AppRoute

class LoginScreenViewModel constructor(
    app: Context,
    private val sPrefManager: SPrefManager,
    private val dataStoreUtil: DataStoreUtil,
    private val loginUseCase: LoginUseCase,
    private val getKeyStatusUseCase: GetKeyStatusUseCase,
    private val rotatePublicKeyUseCase: RotatePublicKeyUseCase,
    private val syncDeviceIdUseCase: SyncDeviceIdUseCase,
    private val messageCrypto: MessageCrypto,
    ) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication){

    val userName = mutableStateOf("")
    val password = mutableStateOf("")

    val loginLoadingState = mutableStateOf(false)
    val loginErrorMessage = mutableStateOf<String?>(null)

    val usernameTextFieldIsError = mutableStateOf(false)
    val passwordTextFieldIsError = mutableStateOf(false)
    val currentLanguage: Flow<AppLanguage> = dataStoreUtil.getLanguage()

    private fun login(username: String, pass: String) = viewModelScope.launch {
        loginUseCase.execute(
            username, pass
        ).collect { data ->
            when(data){
                is Resource.Success -> {
                    sPrefManager.setUsername(username)
                    sPrefManager.setToken(data.data.accessToken)
                    sPrefManager.setRefreshToken(data.data.refreshToken)
                    ensureLocalKeysThenContinue(data.data.accessToken)
                }
                is Resource.Loading -> {
                    loginLoadingState.value = true

                }
                is Resource.Error -> {
                    loginErrorMessage.value =
                        data.message.asString(getApplication<DinoOrderApplication>())
                    usernameTextFieldIsError.value = true
                    passwordTextFieldIsError.value = true
                    loginLoadingState.value = false
                }
            }
        }
    }

    private fun ensureLocalKeysThenContinue(accessToken: String) = viewModelScope.launch {
        if (!isPrivateKeyEmpty() && hasServerPublicKey()) {
            sPrefManager.setUserLogIn(true)
            syncDeviceIdBestEffort()
            loginLoadingState.value = false
            loginErrorMessage.value = null
            getApplication<DinoOrderApplication>()
                .inAppNotificationManager
                .connect(accessToken)
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
                    sPrefManager.savePrivateKey(privateKeyBase64)
                    sPrefManager.savePublicKey(publicKeyBase64)
                    sPrefManager.setUserLogIn(true)
                    syncDeviceIdBestEffort()
                    loginLoadingState.value = false
                    loginErrorMessage.value = null
                    getApplication<DinoOrderApplication>()
                        .inAppNotificationManager
                        .connect(accessToken)
                    navigateToMainScreen()
                }
                is Resource.Error -> {
                    sPrefManager.setUserLogIn(false)
                    sPrefManager.setToken("")
                    sPrefManager.setRefreshToken("")
                    loginLoadingState.value = false
                    loginErrorMessage.value =
                        getApplication<DinoOrderApplication>()
                            .getString(R.string.login_key_recovery_failed)
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
            val nextLanguage = when (dataStoreUtil.getLanguage().first()) {
                AppLanguage.Farsi -> AppLanguage.English
                AppLanguage.English -> AppLanguage.Farsi
            }
            dataStoreUtil.saveLanguage(nextLanguage)
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
        return sPrefManager.getPrivateKey().isNullOrEmpty()
    }

    private fun syncDeviceIdBestEffort() = viewModelScope.launch {
        syncDeviceIdUseCase.execute().collect { _ -> }
    }
}
