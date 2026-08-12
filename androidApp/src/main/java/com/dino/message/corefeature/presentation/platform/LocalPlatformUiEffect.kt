package com.dino.message.corefeature.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

val LocalPlatformUiEffect = staticCompositionLocalOf<PlatformUiEffect> {
    error("PlatformUiEffect is not provided")
}

@Composable
fun rememberAndroidPlatformUiEffect(): PlatformUiEffect {
    val context = LocalContext.current
    return remember(context) { AndroidPlatformUiEffect(context) }
}
