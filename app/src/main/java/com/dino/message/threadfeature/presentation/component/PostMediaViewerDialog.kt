package com.dino.message.threadfeature.presentation.component

import android.net.Uri
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.SubcomposeAsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.dino.message.threadfeature.domain.model.PostMedia
import kotlinx.coroutines.launch

@Composable
fun PostMediaViewerDialog(
    mediaItems: List<PostMedia>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    if (mediaItems.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, mediaItems.lastIndex),
        pageCount = { mediaItems.size }
    )
    val thumbnailListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        thumbnailListState.animateScrollToItem(
            index = pagerState.currentPage,
            scrollOffset = 0
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { mediaItems[it].id }
            ) { page ->
                val item = mediaItems[page]
                val isVideo = item.mime_type.startsWith("video/", ignoreCase = true)

                if (isVideo) {
                    FullScreenVideoPlayer(
                        url = item.url,
                        isCurrentPage = pagerState.currentPage == page,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    FullScreenImage(
                        url = item.url,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            Text(
                text = "${pagerState.currentPage + 1} / ${mediaItems.size}",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
                    .background(
                        Color.Black.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )

            if (mediaItems.size > 1) {
                LazyRow(
                    state = thumbnailListState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(
                        6.dp,
                        Alignment.CenterHorizontally
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    itemsIndexed(
                        items = mediaItems,
                        key = { _, item -> item.id }
                    ) { index, item ->
                        val isActive = pagerState.currentPage == index
                        val borderWidth by animateDpAsState(
                            targetValue = if (isActive) 2.dp else 0.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "thumb-border"
                        )

                        ThumbnailItem(
                            item = item,
                            borderWidth = borderWidth,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThumbnailItem(
    item: PostMedia,
    borderWidth: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val isVideo = item.mime_type.startsWith("video/", ignoreCase = true)
    val modelData: Any = if (
        item.url.startsWith("content://", ignoreCase = true) ||
        item.url.startsWith("file://", ignoreCase = true)
    ) Uri.parse(item.url) else item.url

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(modelData)
            .apply {
                if (isVideo) {
                    decoderFactory(VideoFrameDecoder.Factory())
                    videoFrameMillis(0)
                }
            }
            .crossfade(true)
            .size(120)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(6.dp))
            .then(
                if (borderWidth > 0.dp) {
                    Modifier.border(
                        borderWidth,
                        Color.White,
                        RoundedCornerShape(6.dp)
                    )
                } else Modifier
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun FullScreenImage(
    url: String,
    modifier: Modifier = Modifier
) {
    val modelData: Any = if (
        url.startsWith("content://", ignoreCase = true) ||
        url.startsWith("file://", ignoreCase = true)
    ) Uri.parse(url) else url

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(modelData)
            .crossfade(true)
            .build(),
        contentDescription = "Full-screen image",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun FullScreenVideoPlayer(
    url: String,
    isCurrentPage: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
            prepare()
        }
    }

    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
            }
        },
        update = { playerView ->
            playerView.player = exoPlayer
        }
    )
}
