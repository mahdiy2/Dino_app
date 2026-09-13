package com.dino.message.authfeature.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.dino.message.authfeature.presentation.viewmodel.LoginScreenViewModel
import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.presentation.viewmodel.rememberStateHolder

@Composable
fun LoginScreen() {
    val viewModel: LoginScreenViewModel = rememberStateHolder()
    val loginErrorMessage by viewModel.loginErrorMessage
    val currentLanguage by viewModel.currentLanguage.collectAsState(initial = AppLanguage.Farsi)

    LoginContent(
        username = viewModel.userName.value,
        password = viewModel.password.value,
        usernameIsError = viewModel.usernameTextFieldIsError.value,
        passwordIsError = viewModel.passwordTextFieldIsError.value,
        loginErrorMessage = loginErrorMessage,
        loginLoading = viewModel.loginLoadingState.value,
        currentLanguage = currentLanguage,
        onUsernameChange = viewModel::usernameOnValueChange,
        onPasswordChange = viewModel::passwordOnValueChange,
        onLoginClick = viewModel::onLoginBtnClicked,
        onCreateAccountClick = viewModel::navigateToRegisterScreen,
        onToggleLanguage = viewModel::toggleLanguage,
    )
}
