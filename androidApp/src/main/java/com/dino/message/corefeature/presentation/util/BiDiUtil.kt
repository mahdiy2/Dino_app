package com.dino.message.corefeature.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection

fun detectTextDirection(text: String): LayoutDirection {
    for (char in text) {
        val type = Character.getDirectionality(char).toInt()
        when (type) {
            Character.DIRECTIONALITY_RIGHT_TO_LEFT.toInt(),
            Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC.toInt(),
            Character.DIRECTIONALITY_ARABIC_NUMBER.toInt() -> return LayoutDirection.Rtl
            Character.DIRECTIONALITY_LEFT_TO_RIGHT.toInt() -> return LayoutDirection.Ltr
        }
    }
    return LayoutDirection.Ltr
}

fun resolveTextAlign(text: String): TextAlign {
    return if (detectTextDirection(text) == LayoutDirection.Rtl) TextAlign.Right else TextAlign.Left
}

@Composable
fun rememberTextAlign(text: String): TextAlign {
    return remember(text) { resolveTextAlign(text) }
}

@Composable
fun rememberLayoutDirection(text: String): LayoutDirection {
    return remember(text) { detectTextDirection(text) }
}

@Composable
fun backIconModifier(): Modifier {
    return if (LocalLayoutDirection.current == LayoutDirection.Rtl) {
        Modifier.scale(scaleX = -1f, scaleY = 1f)
    } else {
        Modifier
    }
}
