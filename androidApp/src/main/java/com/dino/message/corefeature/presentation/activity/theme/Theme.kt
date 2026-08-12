package com.dino.message.corefeature.presentation.activity.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.dino.message.corefeature.domain.model.AppLanguage

@Composable
fun SecMessageTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appLanguage: AppLanguage = AppLanguage.English,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) {
            dynamicDarkColorScheme(context = context)
        } else {
            dynamicLightColorScheme(context = context)
        }
    } else {
        secMessageColorScheme(darkTheme)
    }

    SecMessageThemeContent(
        colorScheme = colors,
        appLanguage = appLanguage,
        content = content,
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }
}
