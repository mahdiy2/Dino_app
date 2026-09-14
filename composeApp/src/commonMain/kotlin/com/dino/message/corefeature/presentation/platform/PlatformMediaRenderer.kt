package com.dino.message.corefeature.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale

enum class PlatformImageLoadState {
    Loading,
    Success,
    Error,
}

data class PlatformImageOptions(
    val memoryCacheKey: String? = null,
    val placeholderMemoryCacheKey: String? = null,
    val enableMemoryCache: Boolean = false,
    val enableDiskCache: Boolean = false,
)

class PlatformMediaRenderer(
    val image: @Composable (
        source: String,
        contentDescription: String?,
        modifier: Modifier,
        contentScale: ContentScale,
        options: PlatformImageOptions,
        onState: (PlatformImageLoadState) -> Unit,
    ) -> Unit,
    val preloadImage: @Composable (source: String) -> Unit,
    val video: @Composable (
        source: String,
        modifier: Modifier,
        zoom: Boolean,
        onBufferingChanged: (Boolean) -> Unit,
        onCompleted: () -> Unit,
        onPlaybackError: () -> Unit,
    ) -> Unit,
)

private val NoOpPlatformMediaRenderer = PlatformMediaRenderer(
    image = { _, _, _, _, _, onState ->
        onState(PlatformImageLoadState.Error)
    },
    preloadImage = {},
    video = { _, _, _, _, _, onPlaybackError ->
        onPlaybackError()
    },
)

val LocalPlatformMediaRenderer = staticCompositionLocalOf<PlatformMediaRenderer> {
    NoOpPlatformMediaRenderer
}
