package com.dino.message.corefeature.presentation.platform

import androidx.compose.runtime.remember

@androidx.compose.runtime.Composable
fun rememberAndroidPlatformMediaRenderer(): PlatformMediaRenderer = remember {
    PlatformMediaRenderer(
        image = { source, contentDescription, modifier, contentScale, options, onState ->
        PlatformAsyncImage(
            model = rememberPlatformImageRequest(
                data = source,
                options = PlatformImageRequestOptions(
                    memoryCacheKey = options.memoryCacheKey,
                    placeholderMemoryCacheKey = options.placeholderMemoryCacheKey,
                    enableMemoryCache = options.enableMemoryCache,
                    enableDiskCache = options.enableDiskCache,
                ),
            ),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            onState = { state ->
                onState(
                    when {
                        state.isPlatformImageLoading -> PlatformImageLoadState.Loading
                        state.isPlatformImageError -> PlatformImageLoadState.Error
                        else -> PlatformImageLoadState.Success
                    }
                )
            },
        )
        },
        preloadImage = { source -> PreloadPlatformImage(model = source) },
        video = { source, modifier, zoom, onBufferingChanged, onCompleted, onPlaybackError ->
            PlatformVideoPlayer(
                url = source,
                modifier = modifier,
                useController = false,
                resizeMode = if (zoom) PlatformVideoResizeMode.Zoom else PlatformVideoResizeMode.Fit,
                onBufferingChanged = onBufferingChanged,
                onCompleted = onCompleted,
                onPlaybackError = { onPlaybackError() },
            )
        },
    )
}
