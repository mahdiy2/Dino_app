package com.dino.message.corefeature.presentation.util

import androidx.compose.ui.unit.LayoutDirection
import com.dino.message.corefeature.domain.model.AppLanguage

fun AppLanguage.toLayoutDirection(): LayoutDirection {
    return when (this) {
        AppLanguage.Farsi -> LayoutDirection.Rtl
        AppLanguage.English -> LayoutDirection.Ltr
    }
}
