package com.dino.message.corefeature.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter

@Composable
fun rememberPlatformImagePainter(
    model: Any?,
): AsyncImagePainter {
    return rememberAsyncImagePainter(model = model)
}

val Painter.isPlatformImageLoading: Boolean
    get() = this is AsyncImagePainter && state is AsyncImagePainter.State.Loading
