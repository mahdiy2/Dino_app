package com.dino.message.updatefeature.presentation.screen

import androidx.compose.runtime.Composable
import com.dino.message.corefeature.presentation.platform.LocalPlatformUiEffect
import com.dino.message.corefeature.presentation.platform.PlatformBackHandler
import com.dino.message.corefeature.presentation.viewmodel.rememberStateHolder
import com.dino.message.updatefeature.presentation.viewmodel.AppUpdateViewModel

@Composable
fun AppUpdateScreen(
    isForceUpdate: Boolean,
    title: String,
    message: String,
    @Suppress("UNUSED_PARAMETER")
    downloadUrl: String,
) {
    val viewModel = rememberStateHolder(
        AppUpdateViewModel::class,
        isForceUpdate,
        title,
        message,
        downloadUrl,
    )
    val platformUiEffect = LocalPlatformUiEffect.current

    PlatformBackHandler(enabled = isForceUpdate) {
        // Force-update gate: keep the user on this screen.
    }

    AppUpdateContent(
        isForceUpdate = isForceUpdate,
        title = title,
        message = message,
        onOpenUpdateWebsite = { platformUiEffect.openUrl(UPDATE_WEBSITE_URL) },
        onContinueWithoutUpdate = { viewModel.continueWithoutUpdate() },
    )
}

private const val UPDATE_WEBSITE_URL = "https://api.dinosocial.ir"
