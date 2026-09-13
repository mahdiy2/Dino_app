package com.dino.message.authfeature.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.dino.message.authfeature.presentation.viewmodel.RegisterScreenViewModel
import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.presentation.viewmodel.rememberStateHolder

@Composable
fun RegisterScreen() {
    val viewModel: RegisterScreenViewModel = rememberStateHolder()
    val registerErrorMessage by viewModel.registerErrorMessage
    val currentLanguage by viewModel.currentLanguage.collectAsState(initial = AppLanguage.Farsi)

    RegisterContent(
        username = viewModel.userName.value,
        password = viewModel.password.value,
        referralCode = viewModel.referralCode.value,
        registerErrorMessage = registerErrorMessage,
        registerLoading = viewModel.registerLoadingState.value,
        currentLanguage = currentLanguage,
        isUsernameFormatValid = viewModel::isUsernameFormatValid,
        onUsernameChange = viewModel::onUsernameValueChange,
        onPasswordChange = viewModel::onPasswordValueChange,
        onReferralCodeChange = viewModel::onReferralCodeValueChange,
        onRegisterClick = viewModel::onClickBtnRegister,
        onAlreadyHaveAccountClick = viewModel::navigateToLoginScreen,
        onToggleLanguage = viewModel::toggleLanguage,
    )
}
