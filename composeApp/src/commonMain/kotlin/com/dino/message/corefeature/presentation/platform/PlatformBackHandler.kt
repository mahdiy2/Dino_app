package com.dino.message.corefeature.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

interface PlatformBackHandler {
    @Composable
    fun Handle(enabled: Boolean, onBack: () -> Unit)
}

private object NoOpPlatformBackHandler : PlatformBackHandler {
    @Composable
    override fun Handle(enabled: Boolean, onBack: () -> Unit) = Unit
}

val LocalPlatformBackHandler = staticCompositionLocalOf<PlatformBackHandler> {
    NoOpPlatformBackHandler
}

@Composable
fun PlatformBackHandler(
    enabled: Boolean = true,
    onBack: () -> Unit,
) {
    LocalPlatformBackHandler.current.Handle(
        enabled = enabled,
        onBack = onBack,
    )
}
