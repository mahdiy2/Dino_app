package com.dino.message.corefeature.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.dino.message.corefeature.presentation.viewmodel.PrivateKeyResetDialogViewModel
import com.dino.message.corefeature.presentation.viewmodel.rememberStateHolder

@Composable
fun PrivateKeyResetDialogScreen() {
    val viewModel = rememberStateHolder(PrivateKeyResetDialogViewModel::class)
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    PrivateKeyResetDialogContent(
        isSubmitting = isSubmitting,
        errorMessage = errorMessage,
        onDismiss = viewModel::dismissDialog,
        onConfirmReset = viewModel::confirmReset,
    )
}
