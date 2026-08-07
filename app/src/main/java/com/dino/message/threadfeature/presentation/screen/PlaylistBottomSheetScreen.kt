package com.dino.message.threadfeature.presentation.screen

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dino.message.R
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPTopAppBar
import com.dino.message.threadfeature.domain.model.PlaylistTrack
import com.dino.message.threadfeature.presentation.component.MusicTrackPlayerCard
import com.dino.message.threadfeature.presentation.music.MusicPlaybackManager
import com.dino.message.threadfeature.presentation.music.PlaybackQueueItem
import com.dino.message.threadfeature.presentation.util.artistTextOrUnknown
import com.dino.message.threadfeature.presentation.util.resolveMusicMetadata
import com.dino.message.threadfeature.presentation.viewmodel.PlaylistBottomSheetViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

private const val FOOTER_PRESSED_SCALE = 1.15f
private const val FOOTER_NEIGHBOR_SCALE = 0.92f
private const val FOOTER_REST_SCALE = 1f
private const val FOOTER_POP_DURATION_MS = 220L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistBottomSheetScreen() {
    val lifecycleOwner = LocalLifecycleOwner.current
    if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) return

    val viewModel: PlaylistBottomSheetViewModel = koinViewModel()
    val context = LocalContext.current
    val tracks by viewModel.playlistTracks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val viewerHasBadge by viewModel.viewerHasBadge.collectAsState()
    val playbackState by MusicPlaybackManager.playbackState.collectAsState()
    val hasActiveTrack = playbackState.currentTrackUrl != null
    val snackbarHostState = remember { SnackbarHostState() }
    val playlistListState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var downloadingTrackId by remember { mutableStateOf<Long?>(null) }

    var cardHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val cardHeightDp = with(density) { cardHeightPx.toDp() }

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
        MusicPlaybackManager.playPlaylist(tracks = playbackQueue,
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


    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(DPMedium)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        val (title, button, items) = createRefs()


        Column(
            modifier = Modifier
                .fillMaxWidth()
//                .height(DPTopAppBar)
                .constrainAs(title) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
            }
        ) {
            Text(
                text = stringResource(R.string.playlist_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (tracks.isEmpty()) {
                    stringResource(R.string.playlist_hint_empty)
                } else {
                    stringResource(R.string.playlist_hint_with_count, tracks.size)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }


        Column(
            modifier = Modifier
                .fillMaxSize()

                .constrainAs(items) {
                    top.linkTo(title.bottom, margin = 50.dp)
                    start.linkTo(parent.start)
                    bottom.linkTo(parent.bottom)
                    end.linkTo(parent.end)
                }

//                    .weight(1f, fill = true)
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
                        text = error ?: stringResource(R.string.playlist_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = playlistListState,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(tracks, key = { _, item -> item.id }) { index, track ->
                            val isCurrentTrack = playbackState.currentTrackUrl == track.trackUrl
                            val isPlaying = isCurrentTrack && playbackState.isPlaying

                            SwipeToDeletePlaylistTrackRow(
                                canDownloadTrack = viewerHasBadge,
                                trackId = track.id,
                                isDownloadingTrack = downloadingTrackId == track.id,
                                isAnyDownloadInProgress = downloadingTrackId != null,
                                isSwipeEnabled = !playlistListState.isScrollInProgress,
                                onDelete = { viewModel.stageTrackRemoval(track) },
                                onDownload = {
                                    if (downloadingTrackId != null) return@SwipeToDeletePlaylistTrackRow
                                    downloadingTrackId = track.id
                                    scope.launch {
                                        try {
                                            savePlaylistTrackToDevice(context, track)
                                        } finally {
                                            downloadingTrackId = null
                                        }
                                    }
                                }
                            ) {
                                PlaylistTrackRow(
                                    index = index,
                                    track = track,
                                    isCurrentTrack = isCurrentTrack,
                                    isPlaying = isPlaying,
                                    onRowClick = { playFromIndex(index) },
                                    onPlayPauseClick = {
                                        if (isCurrentTrack) {
                                            MusicPlaybackManager.toggleTrack(track.trackUrl,
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

                        item {
                            val navBarHeightDp = with(density) {
                                WindowInsets.navigationBars.getBottom(this).toDp()
                            }
                            Spacer(Modifier
                                .height(cardHeightDp + navBarHeightDp + navBarHeightDp)
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .constrainAs(button) {
//                        top.linkTo(title.bottom)
                    start.linkTo(parent.start)
                    bottom.linkTo(parent.bottom)
                    end.linkTo(parent.end)
                }
        ) {
            MusicTrackPlayerCard(
                modifier = Modifier.onGloballyPositioned {
                    cardHeightPx = it.size.height
                },
                trackUrl = playbackState.currentTrackUrl.orEmpty(),
                trackTitle = playbackState.title
                    ?: stringResource(R.string.playlist_no_track_playing),
                trackSubtitle = playbackState.artist
                    ?: stringResource(R.string.playlist_tap_track_to_start),
                enabled = hasActiveTrack,
                footerContent = {
                    PlaylistPlaybackFooter(
                        enabled = tracks.isNotEmpty(),
                        isShuffleEnabled = playbackState.isShuffleEnabled,
                        isRepeatEnabled = playbackState.isRepeatEnabled,
                        onShuffleToggle = {
                            MusicPlaybackManager.setShuffleEnabled(!playbackState.isShuffleEnabled
                            )
                        },
                        onRepeatToggle = {
                            MusicPlaybackManager.setRepeatEnabled(!playbackState.isRepeatEnabled
                            )
                        }
                    )
                }
            )

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )
        }


    }


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeletePlaylistTrackRow(
    canDownloadTrack: Boolean,
    trackId: Long,
    isDownloadingTrack: Boolean,
    isAnyDownloadInProgress: Boolean,
    isSwipeEnabled: Boolean,
    onDelete: () -> Unit,
    onDownload: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.6f },
        confirmValueChange = { true }
    )
    LaunchedEffect(trackId) {
        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd =
            isSwipeEnabled || dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd,
        enableDismissFromEndToStart =
            isSwipeEnabled || dismissState.currentValue == SwipeToDismissBoxValue.EndToStart,
        backgroundContent = {
            val showDeleteBackground =
                dismissState.currentValue != SwipeToDismissBoxValue.Settled ||
                    dismissState.targetValue != SwipeToDismissBoxValue.Settled
            val dismissDirection = when {
                dismissState.targetValue == SwipeToDismissBoxValue.EndToStart ||
                    dismissState.currentValue == SwipeToDismissBoxValue.EndToStart -> {
                    SwipeToDismissBoxValue.EndToStart
                }

                dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd ||
                    dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd -> {
                    SwipeToDismissBoxValue.StartToEnd
                }

                else -> SwipeToDismissBoxValue.EndToStart
            }
            val isSwipingToStart = dismissDirection == SwipeToDismissBoxValue.EndToStart
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
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.playlist_cancel_remove_track),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        if (canDownloadTrack) {
                            IconButton(
                                onClick = {
                                    coroutineScope.launch {
                                        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                                    }
                                    onDownload()
                                },
                                enabled = !isAnyDownloadInProgress
                            ) {
                                if (isDownloadingTrack) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Download,
                                        contentDescription = stringResource(R.string.playlist_download_track),
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = onDelete
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = stringResource(R.string.playlist_confirm_remove_track),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        },
        content = content
    )
}

private suspend fun savePlaylistTrackToDevice(
    context: Context,
    track: PlaylistTrack
) {
    try {
        val bytes = withContext(Dispatchers.IO) {
            when {
                track.trackUrl.startsWith("content://", ignoreCase = true) -> {
                    val uri = Uri.parse(track.trackUrl)
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }

                track.trackUrl.startsWith("file://", ignoreCase = true) -> {
                    val uri = Uri.parse(track.trackUrl)
                    uri.path?.let { path -> java.io.File(path).readBytes() }
                }

                else -> {
                    URL(track.trackUrl).openStream().use { it.readBytes() }
                }
            }
        } ?: run {
            Toast.makeText(context, context.getString(R.string.playlist_save_failed), Toast.LENGTH_SHORT).show()
            return
        }

        val extension = audioExtensionForMime(track.mimeType)
        val displayName = track.displayName?.substringBeforeLast('.')?.ifBlank { null }
        val baseName = displayName ?: track.title.ifBlank { "track_${System.currentTimeMillis()}" }
        val fileName = "${baseName}_${System.currentTimeMillis()}.$extension"
        val mimeType = track.mimeType.ifBlank { "audio/mpeg" }

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_MUSIC + "/SecMessage"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
            contentValues
        )

        if (uri == null) {
            Toast.makeText(context, context.getString(R.string.playlist_save_failed), Toast.LENGTH_SHORT).show()
            return
        }

        val writeSucceeded = withContext(Dispatchers.IO) {
            resolver.openOutputStream(uri)?.use { output ->
                output.write(bytes)
            } != null
        }

        if (!writeSucceeded) {
            resolver.delete(uri, null, null)
            Toast.makeText(context, context.getString(R.string.playlist_save_failed), Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }

        Toast.makeText(context, context.getString(R.string.playlist_saved_to_device), Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        Toast.makeText(context, context.getString(R.string.playlist_save_failed), Toast.LENGTH_SHORT).show()
    }
}

private fun audioExtensionForMime(mimeType: String): String {
    val normalized = mimeType.lowercase()
    return when {
        normalized.contains("mpeg") -> "mp3"
        normalized.contains("mp4") -> "m4a"
        normalized.contains("aac") -> "aac"
        normalized.contains("ogg") -> "ogg"
        normalized.contains("wav") -> "wav"
        normalized.contains("flac") -> "flac"
        else -> "mp3"
    }
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
                    contentDescription = if (isPlaying) {
                        stringResource(R.string.playlist_pause_track)
                    } else {
                        stringResource(R.string.playlist_play_track)
                    }
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
                contentDescription = stringResource(R.string.playlist_toggle_shuffle)
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(text = stringResource(R.string.playlist_shuffle))
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
                contentDescription = stringResource(R.string.playlist_toggle_repeat)
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(text = stringResource(R.string.playlist_repeat))
        }
    }
}
