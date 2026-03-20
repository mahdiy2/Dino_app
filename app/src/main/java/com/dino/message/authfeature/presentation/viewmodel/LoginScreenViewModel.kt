package com.dino.message.authfeature.presentation.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.dino.message.chatfeature.data.worker.UnreadPollingScheduler
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.authfeature.domain.usecase.LoginUseCase
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.presentation.util.PopBackStackLevel
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.ramcosta.composedestinations.generated.destinations.MainScreenDestination
import com.ramcosta.composedestinations.generated.destinations.RegisterScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ThreadScreenDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginScreenViewModel @Inject constructor(
    @ApplicationContext app: Context,
    private val sPrefManager: SPrefManager,
    private val loginUseCase: LoginUseCase,
    ) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication){

    val userName = mutableStateOf("")
    val password = mutableStateOf("")

    val privateKey = mutableStateOf("")

    val loginLoadingState = mutableStateOf(false)

    val usernameTextFieldIsError = mutableStateOf(false)
    val passwordTextFieldIsError = mutableStateOf(false)

    val privateKeyTextFieldIsError = mutableStateOf(false)


    private fun login(username: String, pass: String, privateKey: String) = viewModelScope.launch {
        if (isPrivateKeyEmpty()){
            if (privateKey.isEmpty()){
                privateKeyTextFieldIsError.value = true
            } else {
                sPrefManager.savePrivateKey(privateKey)
            }
        }
        loginUseCase.execute(
            username, pass
        ).collect { data ->
            when(data){
                is Resource.Success -> {
                    loginLoadingState.value = false
                    sPrefManager.setUserLogIn(true)
                    sPrefManager.setUsername(username)
                    sPrefManager.setToken(data.data.accessToken)
                    sPrefManager.setRefreshToken(data.data.refreshToken)
                    UnreadPollingScheduler.schedule(getApplication<DinoOrderApplication>())
                    navigateToMainScreen()
                }
                is Resource.Loading -> {
                    loginLoadingState.value = true

                }
                is Resource.Error -> {
                    usernameTextFieldIsError.value = true
                    passwordTextFieldIsError.value = true
                    loginLoadingState.value = false

                }
            }
        }
    }

    fun usernameOnValueChange(value: String){
        usernameTextFieldIsError.value = false
        passwordTextFieldIsError.value = false
        userName.value = value
    }

    fun passwordOnValueChange(value: String){
        usernameTextFieldIsError.value = false
        passwordTextFieldIsError.value = false
        password.value = value
    }

    fun privateKeyOnValueChange(value: String){
        privateKeyTextFieldIsError.value = false
        privateKey.value = value
    }

    fun onLoginBtnClicked(){
        login(userName.value, password.value, privateKey.value)
    }

    fun navigateToRegisterScreen(){
        navigateToDestination(RegisterScreenDestination, viewModelScope)
    }

    fun navigateToMainScreen(){
        navigateWithPopBackStackToDestination(
            ThreadScreenDestination,
            viewModelScope,
            PopBackStackLevel.ALL
        )
    }

    fun setPrivateKey(key: String){
        sPrefManager.savePrivateKey(key)
    }

    fun isPrivateKeyEmpty(): Boolean {
        return sPrefManager.getPrivateKey().isNullOrEmpty()
    }
}
