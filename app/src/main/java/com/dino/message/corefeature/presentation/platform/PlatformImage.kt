package com.dino.message.corefeature.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.rememberAsyncImagePainter
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import coil3.video.videoFrameMillis

@Composable
fun rememberPlatformImagePainter(
    model: Any?,
): AsyncImagePainter {
    return rememberAsyncImagePainter(model = model)
}

val AsyncImagePainter.isPlatformImageLoading: Boolean
    get() = state.value is AsyncImagePainter.State.Loading

val Any?.isPlatformImageLoading: Boolean
    get() = this is AsyncImagePainter.State.Loading

val Any?.isPlatformImageError: Boolean
    get() = this is AsyncImagePainter.State.Error

data class PlatformImageRequestOptions(
    val crossfade: Boolean = false,
    val memoryCacheKey: String? = null,
    val placeholderMemoryCacheKey: String? = null,
    val enableMemoryCache: Boolean = false,
    val enableDiskCache: Boolean = false,
    val videoFrameMillis: Long? = null,
    val sizePx: Int? = null,
)

@Composable
fun rememberPlatformImageRequest(
    data: Any?,
    options: PlatformImageRequestOptions = PlatformImageRequestOptions(),
): ImageRequest {
    val context = LocalContext.current
    return remember(context, data, options) {
        ImageRequest.Builder(context)
            .data(data)
            .apply {
                crossfade(options.crossfade)
                options.memoryCacheKey?.let(::memoryCacheKey)
                options.placeholderMemoryCacheKey?.let(::placeholderMemoryCacheKey)
                if (options.enableDiskCache) {
                    diskCachePolicy(CachePolicy.ENABLED)
                }
                if (options.enableMemoryCache) {
                    memoryCachePolicy(CachePolicy.ENABLED)
                }
                options.videoFrameMillis?.let {
                    decoderFactory(VideoFrameDecoder.Factory())
                    videoFrameMillis(it)
                }
                options.sizePx?.let(::size)
            }
            .build()
    }
}

@Composable
fun PlatformAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    onState: ((Any?) -> Unit)? = null,
) {
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        onState = { state -> onState?.invoke(state) },
    )
}

@Composable
fun PlatformSubcomposeAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    onError: ((Throwable) -> Unit)? = null,
    loading: @Composable (() -> Unit)? = null,
    error: @Composable (() -> Unit)? = null,
) {
    SubcomposeAsyncImage(
        model = model,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        onError = { state -> onError?.invoke(state.result.throwable) },
        loading = loading?.let { loadingContent -> { loadingContent() } },
        error = error?.let { errorContent -> { errorContent() } },
    )
}

@Composable
fun PreloadPlatformImage(model: Any?) {
    if (model == null) return
    val context = LocalContext.current
    val imageRequest = rememberPlatformImageRequest(data = model)
    LaunchedEffect(imageRequest) {
        context.imageLoader.enqueue(imageRequest)
    }
}
