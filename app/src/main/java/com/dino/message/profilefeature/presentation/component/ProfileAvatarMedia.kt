package com.dino.message.profilefeature.presentation.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.SubcomposeAsyncImage
import coil.imageLoader
import coil.request.ImageRequest

@Composable
fun ProfileAvatarMedia(
    profileImageUrl: String?,
    profileVideoUrl: String?,
    modifier: Modifier = Modifier
) {
    val normalizedImageUrl = profileImageUrl?.takeIf { it.isNotBlank() }
    val normalizedVideoUrl = profileVideoUrl?.takeIf { it.isNotBlank() }
    var hasVideoFinished by remember(normalizedVideoUrl, normalizedImageUrl) {
        mutableStateOf(normalizedVideoUrl == null)
    }
    val shouldShowVideo = normalizedVideoUrl != null && !hasVideoFinished

    PreloadProfileImage(
        imageUrl = normalizedImageUrl,
        enabled = shouldShowVideo
    )

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        if (shouldShowVideo) {
            ProfileVideoAutoPlayer(
                url = normalizedVideoUrl!!,
                modifier = Modifier.fillMaxSize(),
                onFinished = { hasVideoFinished = true }
            )
        } else {
            ProfileImageAvatar(
                imageUrl = normalizedImageUrl,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun PreloadProfileImage(
    imageUrl: String?,
    enabled: Boolean
) {
    if (!enabled || imageUrl == null) return
    val context = LocalContext.current
    LaunchedEffect(imageUrl) {
        context.imageLoader.enqueue(
            ImageRequest.Builder(context)
                .data(imageUrl)
                .build()
        )
    }
}

@Composable
private fun ProfileImageAvatar(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    if (imageUrl == null) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(true)
            .build(),
        contentDescription = "Profile image",
        contentScale = ContentScale.Crop,
        modifier = modifier,
        loading = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            }
        },
        error = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun ProfileVideoAutoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val latestOnFinished by rememberUpdatedState(onFinished)
    val exoPlayer = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            repeatMode = Player.REPEAT_MODE_OFF
            playWhenReady = true
            prepare()
        }
    }
    var isBuffering by remember(url) { mutableStateOf(true) }

    DisposableEffect(exoPlayer) {
        var hasFinished = false
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering =
                    playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                if (playbackState == Player.STATE_ENDED && !hasFinished) {
                    hasFinished = true
                    latestOnFinished()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                if (!hasFinished) {
                    hasFinished = true
                    latestOnFinished()
                }
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
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}