package com.dino.message.corefeature.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.dino.message.threadfeature.presentation.music.MusicPlaybackManager
import kotlinx.coroutines.delay

enum class PlatformVideoResizeMode {
    Fit,
    Zoom,
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlatformVideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    useController: Boolean = true,
    repeat: Boolean = false,
    resizeMode: PlatformVideoResizeMode = PlatformVideoResizeMode.Fit,
    playWhenReady: Boolean = true,
    isPaused: Boolean = false,
    pauseCompetingAudio: Boolean = true,
    transparentShutter: Boolean = false,
    onBufferingChanged: (Boolean) -> Unit = {},
    onDurationKnown: (Long) -> Unit = {},
    onStarted: () -> Unit = {},
    onProgress: (Long) -> Unit = {},
    onCompleted: () -> Unit = {},
    onPlaybackError: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val latestOnBufferingChanged by rememberUpdatedState(onBufferingChanged)
    val latestOnDurationKnown by rememberUpdatedState(onDurationKnown)
    val latestOnStarted by rememberUpdatedState(onStarted)
    val latestOnProgress by rememberUpdatedState(onProgress)
    val latestOnCompleted by rememberUpdatedState(onCompleted)
    val latestOnPlaybackError by rememberUpdatedState(onPlaybackError)
    val exoPlayer = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            repeatMode = if (repeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            this.playWhenReady = playWhenReady && !isPaused
            prepare()
        }
    }

    LaunchedEffect(url, pauseCompetingAudio) {
        if (pauseCompetingAudio) {
            MusicPlaybackManager.pauseForCompetingAudio()
        }
    }

    LaunchedEffect(exoPlayer, isPaused, playWhenReady) {
        if (isPaused || !playWhenReady) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            val duration = exoPlayer.duration
            if (duration > 0L) {
                latestOnDurationKnown(duration)
            }
            latestOnProgress(exoPlayer.currentPosition.coerceAtLeast(0L))
            delay(50L)
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                latestOnBufferingChanged(
                    playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                )
                if (playbackState == Player.STATE_ENDED) {
                    latestOnCompleted()
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    latestOnStarted()
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                latestOnPlaybackError(
                    error.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "Video playback failed."
                )
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                this.useController = useController
                this.resizeMode = resizeMode.toMedia3ResizeMode()
                if (transparentShutter) {
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
                player = exoPlayer
            }
        },
        update = { playerView ->
            playerView.useController = useController
            playerView.resizeMode = resizeMode.toMedia3ResizeMode()
            playerView.player = exoPlayer
        }
    )
}

@Composable
fun rememberPlatformVideoBufferingState(url: String): Pair<Boolean, (Boolean) -> Unit> {
    var isBuffering by remember(url) { mutableStateOf(true) }
    return isBuffering to { buffering -> isBuffering = buffering }
}

private fun PlatformVideoResizeMode.toMedia3ResizeMode(): Int =
    when (this) {
        PlatformVideoResizeMode.Fit -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        PlatformVideoResizeMode.Zoom -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    }
