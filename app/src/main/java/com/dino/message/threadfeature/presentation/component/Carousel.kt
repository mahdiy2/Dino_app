package com.dino.message.threadfeature.presentation.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.dino.message.threadfeature.domain.model.PostMedia

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Carousel(
    modifier: Modifier = Modifier,
    items: List<PostMedia>,
) {
    if (items.isEmpty()) return

    var activeVideoId by remember(items) { mutableStateOf<Long?>(null) }

    LaunchedEffect(items, activeVideoId) {
        if (activeVideoId != null && items.none { it.id == activeVideoId }) {
            activeVideoId = null
        }
    }

    HorizontalMultiBrowseCarousel(
        state = rememberCarouselState { items.count() },
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(top = 16.dp, bottom = 16.dp),
        preferredItemWidth = 186.dp,
        itemSpacing = 8.dp,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) { i ->
        val item = items[i]
        val isVideo = item.isVideoMedia()

        MediaItemCard(
            modifier = Modifier
                .height(205.dp)
                .maskClip(MaterialTheme.shapes.extraLarge),
            item = item,
            isVideo = isVideo,
            isPlaying = isVideo && activeVideoId == item.id,
            onVideoClick = {
                activeVideoId = if (activeVideoId == item.id) null else item.id
            },
            onVideoStopRequest = { stopId ->
                if (activeVideoId == stopId) {
                    activeVideoId = null
                }
            }
        )
    }
}

@Composable
private fun MediaItemCard(
    modifier: Modifier,
    item: PostMedia,
    isVideo: Boolean,
    isPlaying: Boolean,
    onVideoClick: () -> Unit,
    onVideoStopRequest: (Long) -> Unit
) {
    val localView = LocalView.current
    var visibleInWindow by remember(item.id) { mutableStateOf(true) }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                val windowWidth = localView.width.toFloat()
                val windowHeight = localView.height.toFloat()
                visibleInWindow = bounds.bottom > 0f &&
                    bounds.right > 0f &&
                    bounds.top < windowHeight &&
                    bounds.left < windowWidth
            }
            .then(
                if (isVideo) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onVideoClick
                    )
                } else {
                    Modifier
                }
            )
    ) {
        if (isVideo && isPlaying) {
            VideoPlayer(
                url = item.url,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            MediaImage(
                url = item.url,
                modifier = Modifier.fillMaxSize(),
                contentDescription = "Post media ${item.id}",
                isVideo = isVideo
            )
        }

        if (isVideo) {
            VideoBadge(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            )
        }
    }

    LaunchedEffect(visibleInWindow, isPlaying) {
        if (isPlaying && !visibleInWindow) {
            onVideoStopRequest(item.id)
        }
    }

    DisposableEffect(isPlaying) {
        onDispose {
            if (isPlaying) {
                onVideoStopRequest(item.id)
            }
        }
    }
}

@Composable
private fun MediaImage(
    url: String,
    modifier: Modifier,
    contentDescription: String,
    isVideo: Boolean
) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .apply {
                if (isVideo) {
                    videoFrameMillis(1_000)
                }
            }
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        loading = {
            MediaLoading(modifier = Modifier.fillMaxSize())
        },
        error = {
            MediaError(modifier = Modifier.fillMaxSize())
        }
    )
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoPlayer(
    url: String,
    modifier: Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            prepare()
        }
    }
    var isBuffering by remember { mutableStateOf(true) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PlayerView(viewContext).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    player = exoPlayer
                }
            },
            update = { playerView ->
                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                playerView.player = exoPlayer
            }
        )

        if (isBuffering) {
            MediaLoading(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun VideoBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)
    ) {
        Icon(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            imageVector = Icons.Rounded.Videocam,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun MediaLoading(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "media-loading")
    val xShimmer by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "media-loading-shimmer"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
        ),
        start = Offset(x = xShimmer - 260f, y = 0f),
        end = Offset(x = xShimmer, y = 260f)
    )

    Box(
        modifier = modifier.background(shimmerBrush),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun MediaError(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.BrokenImage,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun PostMedia.isVideoMedia(): Boolean {
    return mime_type.startsWith("video/", ignoreCase = true)
}
