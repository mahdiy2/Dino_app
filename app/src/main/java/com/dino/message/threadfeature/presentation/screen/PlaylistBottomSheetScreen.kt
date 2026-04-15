package com.dino.message.threadfeature.presentation.screen

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.threadfeature.domain.model.PlaylistTrack
import com.dino.message.threadfeature.presentation.component.MusicTrackPlayerCard
import com.dino.message.threadfeature.presentation.music.MusicPlaybackManager
import com.dino.message.threadfeature.presentation.music.PlaybackQueueItem
import com.dino.message.threadfeature.presentation.util.artistTextOrUnknown
import com.dino.message.threadfeature.presentation.util.resolveMusicMetadata
import com.dino.message.threadfeature.presentation.viewmodel.PlaylistBottomSheetViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.bottomsheet.spec.DestinationStyleBottomSheet
import kotlinx.coroutines.delay

private const val FOOTER_PRESSED_SCALE = 1.15f
private const val FOOTER_NEIGHBOR_SCALE = 0.92f
private const val FOOTER_REST_SCALE = 1f
private const val FOOTER_POP_DURATION_MS = 220L

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = DestinationStyleBottomSheet::class)
@Composable
fun PlaylistBottomSheetScreen() {
    val lifecycleOwner = LocalLifecycleOwner.current
    if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) return

    val viewModel: PlaylistBottomSheetViewModel = hiltViewModel()
    val context = LocalContext.current
    val tracks by viewModel.playlistTracks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val playbackState by MusicPlaybackManager.playbackState.collectAsState()
    val hasActiveTrack = playbackState.currentTrackUrl != null
    val snackbarHostState = remember { SnackbarHostState() }

    val playbackQueue = remember(tracks) {
        tracks.map { track ->
            PlaybackQueueItem(
                trackUrl = track.trackUrl,
                title = track.title,
                artist = track.artist
            )
        }
    }

    fun playFromIndex(index: Int) {
        if (playbackQueue.isEmpty()) return
        MusicPlaybackManager.playPlaylist(
            context = context,
            tracks = playbackQueue,
            startIndex = index
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { event ->
            val result = snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = event.actionLabel,
                duration = SnackbarDuration.Short
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                event.trackId?.let(viewModel::undoTrackRemoval)
            } else {
                event.trackId?.let(viewModel::clearPendingRemoval)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(DPMedium)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(DPMedium)
        ) {
            Text(
                text = "Playlist",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (tracks.isEmpty()) {
                    "Tap a track to play."
                } else {
                    "${tracks.size} tracks • Swipe left or right to remove."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    tracks.isEmpty() -> {
                        Text(
                            text = error ?: "Your playlist is empty.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(tracks, key = { _, item -> item.id }) { index, track ->
                                val isCurrentTrack = playbackState.currentTrackUrl == track.trackUrl
                                val isPlaying = isCurrentTrack && playbackState.isPlaying

                                SwipeToDeletePlaylistTrackRow(
                                    trackId = track.id,
                                    onDelete = { viewModel.stageTrackRemoval(track) }
                                ) {
                                    PlaylistTrackRow(
                                        index = index,
                                        track = track,
                                        isCurrentTrack = isCurrentTrack,
                                        isPlaying = isPlaying,
                                        onRowClick = { playFromIndex(index) },
                                        onPlayPauseClick = {
                                            if (isCurrentTrack) {
                                                MusicPlaybackManager.toggleTrack(
                                                    context = context,
                                                    trackUrl = track.trackUrl,
                                                    title = track.title,
                                                    artist = track.artist
                                                )
                                            } else {
                                                playFromIndex(index)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            MusicTrackPlayerCard(
                trackUrl = playbackState.currentTrackUrl.orEmpty(),
                trackTitle = playbackState.title ?: "No track playing",
                trackSubtitle = playbackState.artist ?: "Tap a playlist track to start playback",
                enabled = hasActiveTrack,
                footerContent = {
                    PlaylistPlaybackFooter(
                        enabled = tracks.isNotEmpty(),
                        isShuffleEnabled = playbackState.isShuffleEnabled,
                        isRepeatEnabled = playbackState.isRepeatEnabled,
                        onShuffleToggle = {
                            MusicPlaybackManager.setShuffleEnabled(
                                context = context,
                                enabled = !playbackState.isShuffleEnabled
                            )
                        },
                        onRepeatToggle = {
                            MusicPlaybackManager.setRepeatEnabled(
                                context = context,
                                enabled = !playbackState.isRepeatEnabled
                            )
                        }
                    )
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeletePlaylistTrackRow(
    trackId: Long,
    onDelete: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.25f },
        confirmValueChange = { targetValue ->
            when (targetValue) {
                SwipeToDismissBoxValue.StartToEnd,
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }

                SwipeToDismissBoxValue.Settled -> true
            }
        }
    )
    LaunchedEffect(trackId) {
        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val showDeleteBackground =
                dismissState.currentValue != SwipeToDismissBoxValue.Settled ||
                    dismissState.targetValue != SwipeToDismissBoxValue.Settled
            val isSwipingToStart = dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart
            val alignment = if (isSwipingToStart) Alignment.CenterEnd else Alignment.CenterStart
            val backgroundColor = if (showDeleteBackground) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                Color.Transparent
            }

            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = alignment
            ) {
                Surface(
                    color = backgroundColor,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.fillMaxSize()
                ) {}

                if (showDeleteBackground) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Delete track",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        },
        content = content
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlaylistTrackRow(
    index: Int,
    track: PlaylistTrack,
    isCurrentTrack: Boolean,
    isPlaying: Boolean,
    onRowClick: () -> Unit,
    onPlayPauseClick: () -> Unit
) {
    val resolvedMetadata = remember(track) {
        resolveMusicMetadata(
            displayName = track.displayName,
            title = track.title,
            artist = track.artist
        )
    }
    val cookieShape = MaterialShapes.Pill.toShape()
    val playButtonShapes = remember(cookieShape) {
        ButtonShapes(shape = cookieShape, pressedShape = cookieShape)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRowClick),
        shape = MaterialTheme.shapes.extraLarge,
        color = if (isCurrentTrack) {
            MaterialTheme.colorScheme.surfaceContainerHighest
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DPMedium, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(DPMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${index + 1}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isCurrentTrack) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = artistTextOrUnknown(resolvedMetadata.artist),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FilledTonalButton(
                onClick = onPlayPauseClick,
                shapes = playButtonShapes,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause track" else "Play track"
                )
            }
        }
    }
}

@Composable
private fun PlaylistPlaybackFooter(
    enabled: Boolean,
    isShuffleEnabled: Boolean,
    isRepeatEnabled: Boolean,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit
) {
    var activeFooterButton by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(activeFooterButton) {
        if (activeFooterButton == null) return@LaunchedEffect
        delay(FOOTER_POP_DURATION_MS)
        activeFooterButton = null
    }

    val bouncyFloatSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
    val bouncyDpSpring = spring<Dp>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
    val shuffleScale by animateFloatAsState(
        targetValue = when {
            activeFooterButton == 0 -> FOOTER_PRESSED_SCALE
            activeFooterButton == 1 -> FOOTER_NEIGHBOR_SCALE
            else -> FOOTER_REST_SCALE
        },
        animationSpec = bouncyFloatSpring,
        label = "shuffleScale"
    )
    val repeatScale by animateFloatAsState(
        targetValue = when {
            activeFooterButton == 1 -> FOOTER_PRESSED_SCALE
            activeFooterButton == 0 -> FOOTER_NEIGHBOR_SCALE
            else -> FOOTER_REST_SCALE
        },
        animationSpec = bouncyFloatSpring,
        label = "repeatScale"
    )
    val shuffleOffsetX by animateDpAsState(
        targetValue = if (activeFooterButton == 1) (-3).dp else 0.dp,
        animationSpec = bouncyDpSpring,
        label = "shuffleOffsetX"
    )
    val repeatOffsetX by animateDpAsState(
        targetValue = if (activeFooterButton == 0) 3.dp else 0.dp,
        animationSpec = bouncyDpSpring,
        label = "repeatOffsetX"
    )


    val shuffleCorner by animateDpAsState(
        targetValue = if (isShuffleEnabled) 12.dp else 50.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "shuffleCorner"
    )

    val repeatCorner by animateDpAsState(
        targetValue = if (isRepeatEnabled) 12.dp else 50.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "repeatCorner"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        FilledTonalButton(
            onClick = {
                activeFooterButton = 0
                onShuffleToggle()
            },
            shape = RoundedCornerShape(shuffleCorner),
            enabled = enabled,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (isShuffleEnabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                contentColor = if (isShuffleEnabled) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            ),
            modifier = Modifier
                .offset(x = shuffleOffsetX)
                .graphicsLayer {
                    scaleX = shuffleScale
                    scaleY = shuffleScale
                }
        ) {
            Icon(
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = "Toggle shuffle"
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(text = "Shuffle")
        }

        FilledTonalButton(
            onClick = {
                activeFooterButton = 1
                onRepeatToggle()
            },
            shape = RoundedCornerShape(repeatCorner),
            enabled = enabled,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = if (isRepeatEnabled) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                contentColor = if (isRepeatEnabled) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            ),
            modifier = Modifier
                .offset(x = repeatOffsetX)
                .graphicsLayer {
                    scaleX = repeatScale
                    scaleY = repeatScale
                }
        ) {
            Icon(
                imageVector = Icons.Rounded.Repeat,
                contentDescription = "Toggle repeat"
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(text = "Repeat")
        }
    }
}
