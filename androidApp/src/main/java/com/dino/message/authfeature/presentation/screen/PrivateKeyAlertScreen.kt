package com.dino.message.authfeature.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.dino.message.authfeature.presentation.viewmodel.PrivateKeyAlertScreenViewModel
import com.dino.message.corefeature.presentation.viewmodel.rememberStateHolder

@Composable
fun PrivateKeyAlertScreen() {
    val viewModel: PrivateKeyAlertScreenViewModel = rememberStateHolder()
    val clipboardManager = LocalClipboardManager.current
    val confirmErrorMessage by viewModel.confirmErrorMessage
    val isConfirmLoading by viewModel.confirmLoadingState
    var privateKey by remember { mutableStateOf(viewModel.getPendingPrivateKey()) }
    var hasCopied by rememberSaveable { mutableStateOf(false) }
    var showFullKey by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = true) {
        // Block back navigation so the user cannot bypass key backup confirmation.
    }

    PrivateKeyAlertContent(
        privateKey = privateKey,
        hasCopied = hasCopied,
        showFullKey = showFullKey,
        confirmErrorMessage = confirmErrorMessage,
        isConfirmLoading = isConfirmLoading,
        onRetryPrivateKey = { privateKey = viewModel.getPendingPrivateKey() },
        onBackToRegister = viewModel::navigateToRegisterScreenAndResetPending,
        onCopyPrivateKey = {
            clipboardManager.setText(AnnotatedString(privateKey.orEmpty()))
            hasCopied = true
        },
        onToggleShowFullKey = { showFullKey = !showFullKey },
        onContinue = { viewModel.onContinueClicked(hasCopied = hasCopied) },
    )
}
