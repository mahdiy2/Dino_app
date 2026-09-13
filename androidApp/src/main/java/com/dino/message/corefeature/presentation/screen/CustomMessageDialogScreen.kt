package com.dino.message.corefeature.presentation.screen

import androidx.compose.runtime.Composable
import com.dino.message.corefeature.domain.model.CustomMessageDialogData
import com.dino.message.corefeature.presentation.viewmodel.CustomMessageDialogViewModel
import com.dino.message.corefeature.presentation.viewmodel.rememberStateHolder

@Composable
fun CustomMessageDialogScreen(customMessageDialogData: CustomMessageDialogData) {
    val viewModel: CustomMessageDialogViewModel = rememberStateHolder(customMessageDialogData.type)

    CustomMessageDialogContent(
        customMessageDialogData = customMessageDialogData,
        onDismiss = viewModel::navigateUp,
    )
}
