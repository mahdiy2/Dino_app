package com.dino.message.threadfeature.presentation.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonShapes
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.dino.message.R
import com.dino.message.corefeature.presentation.platform.PlatformImageRequestOptions
import com.dino.message.corefeature.presentation.platform.PlatformSubcomposeAsyncImage
import com.dino.message.corefeature.presentation.platform.rememberPlatformImageRequest
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.util.resolveProfileImageShape
import com.dino.message.threadfeature.domain.model.Author
import com.dino.message.threadfeature.presentation.music.MusicPlaybackManager
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.sin
import com.dino.message.corefeature.presentation.icons.AppIcons

private const val PRESSED_SCALE = 1.15f
private const val NEIGHBOR_SCALE = 0.92f
private const val REST_SCALE = 1f
private const val POP_DURATION_MS = 220L
private val PlaylistHeaderActionHeight = 40.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MusicTrackPlayerCard(
    trackUrl: String,
    trackTitle: String,
    trackSubtitle: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    playlistAdders: List<Author> = emptyList(),
    onPlaylistAddersClick: (() -> Unit)? = null,
    onAddToPlaylistClick: (() -> Unit)? = null,
    addActionLabel: String? = null,
    footerContent: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val resolvedAddActionLabel = addActionLabel ?: stringResource(R.string.music_queue_add)
    val playbackState by MusicPlaybackManager.playbackState.collectAsState()
    val isCurrentTrack = playbackState.currentTrackUrl == trackUrl
    val isPlaying = isCurrentTrack && playbackState.isPlaying
    val durationMs = if (isCurrentTrack) playbackState.durationMs else 0L
    val currentPositionMs = if (isCurrentTrack) playbackState.positionMs else 0L

    var sliderValue by remember(trackUrl) { mutableFloatStateOf(0f) }
    var isSliderDragging by remember(trackUrl) { mutableStateOf(false) }
    var activeButton by remember(trackUrl) { mutableStateOf<Int?>(null) }

    val sliderMax = durationMs.coerceAtLeast(1L).toFloat()
    if (!isSliderDragging) {
        sliderValue = currentPositionMs.coerceAtLeast(0L).toFloat()
    }


    LaunchedEffect(activeButton) {
        if (activeButton == null) return@LaunchedEffect
        delay(POP_DURATION_MS)
        activeButton = null
    }

    val sideShape = MaterialShapes.Cookie12Sided.toShape()
    val sidePressedShape = MaterialShapes.Circle.toShape()
    val sideButtonShapes = remember(sideShape, sidePressedShape) {
        ButtonShapes(shape = sideShape, pressedShape = sideShape)
    }

    val bouncyFloatSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
    val bouncyDpSpring = spring<Dp>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    val rewindScale by animateFloatAsState(
        targetValue = when {
            activeButton == 0 -> PRESSED_SCALE
            isNeighborButton(activeButton = activeButton, index = 0) -> NEIGHBOR_SCALE
            else -> REST_SCALE
        },
        animationSpec = bouncyFloatSpring,
        label = "rewindScale"
    )
    val playScale by animateFloatAsState(
        targetValue = when {
            activeButton == 1 -> PRESSED_SCALE
            isNeighborButton(activeButton = activeButton, index = 1) -> NEIGHBOR_SCALE
            else -> REST_SCALE
        },
        animationSpec = bouncyFloatSpring,
        label = "playScale"
    )
    val forwardScale by animateFloatAsState(
        targetValue = when {
            activeButton == 2 -> PRESSED_SCALE
            isNeighborButton(activeButton = activeButton, index = 2) -> NEIGHBOR_SCALE
            else -> REST_SCALE
        },
        animationSpec = bouncyFloatSpring,
        label = "forwardScale"
    )
    val rewindOffsetX by animateDpAsState(
        targetValue = if (activeButton == 1) (-4).dp else 0.dp,
        animationSpec = bouncyDpSpring,
        label = "rewindOffsetX"
    )
    val playOffsetX by animateDpAsState(
        targetValue = when (activeButton) {
            0 -> 3.dp
            2 -> (-3).dp
            else -> 0.dp
        },
        animationSpec = bouncyDpSpring,
        label = "playOffsetX"
    )
    val forwardOffsetX by animateDpAsState(
        targetValue = if (activeButton == 1) 4.dp else 0.dp,
        animationSpec = bouncyDpSpring,
        label = "forwardOffsetX"
    )
    val addersCount = playlistAdders.size
    val shouldShowPlaylistAddersButton = addersCount > 0 && onPlaylistAddersClick != null

    Surface(
        modifier = modifier
            .fillMaxWidth(),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
//            shape = MaterialTheme.shapes.extraLarge
        ),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(
                top = DPMedium,
                end = DPExtraLarge,
                start = DPExtraLarge,
                bottom = DPExtraLarge
            ),
            verticalArrangement = Arrangement.spacedBy(DPLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (onAddToPlaylistClick != null) {
                if (shouldShowPlaylistAddersButton) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalButton(
                            onClick = { onPlaylistAddersClick?.invoke() },
                            enabled = enabled,
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier.height(PlaylistHeaderActionHeight)
                        ) {
                            PlaylistAddersButtonContent(
                                adders = playlistAdders,
                                addersCount = addersCount
                            )
                        }
                        Card(
                            onClick = onAddToPlaylistClick,
                            enabled = enabled,
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier.height(PlaylistHeaderActionHeight)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    modifier = Modifier
                                        .padding(horizontal = DPExtraLarge, vertical = DPMedium),
                                    text = resolvedAddActionLabel,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = AppIcons.MusicNote, contentDescription = null)
                        Card(
                            onClick = onAddToPlaylistClick,
                            enabled = enabled,
                            shape = MaterialTheme.shapes.extraLarge
                        ) {
                            Column(
                                modifier = Modifier.fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    modifier = Modifier
                                        .padding(horizontal = DPExtraLarge, vertical = DPMedium),
                                    text = resolvedAddActionLabel,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                        }
                    }
                }
            }
            Text(
                text = trackTitle,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
            if (!trackSubtitle.isNullOrBlank()) {
                Text(
                    text = trackSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        DPMedium,
                        Alignment.CenterHorizontally
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            activeButton = 0
                            MusicPlaybackManager.seekBy(-10_000L)
                        },
                        enabled = enabled && isCurrentTrack,
                        shapes = sideButtonShapes,
                        modifier = Modifier
                            .offset(x = rewindOffsetX)
                            .size(56.dp)
                            .graphicsLayer {
                                scaleX = rewindScale
                                scaleY = rewindScale
                            },
                    ) {
                        Icon(
                            imageVector = AppIcons.SkipPrevious,
                            contentDescription = stringResource(R.string.music_seek_backward_10),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Button(
                        onClick = {
                            activeButton = 1
                            if (!enabled) return@Button
                            MusicPlaybackManager.toggleTrack(trackUrl,
                                title = trackTitle,
                                artist = trackSubtitle
                            )
                        },
                        enabled = enabled,
                        shapes = ButtonDefaults.shapes(
                            MaterialTheme.shapes.extraLarge
                        ),
                        modifier = Modifier
                            .offset(x = playOffsetX)
                            .height(56.dp)
                            .width(120.dp)
//                        .size(72.dp)
                            .graphicsLayer {
                                scaleX = playScale
                                scaleY = playScale
                            }
                    ) {
                        Icon(
                            imageVector = if (isPlaying) {
                                AppIcons.Pause
                            } else {
                                AppIcons.PlayArrow
                            },
                            contentDescription = if (isPlaying) {
                                stringResource(R.string.pause)
                            } else {
                                stringResource(R.string.play)
                            },
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            activeButton = 2
                            MusicPlaybackManager.seekBy(10_000L)
                        },
                        enabled = enabled && isCurrentTrack,
                        shapes = sideButtonShapes,
                        modifier = Modifier
                            .offset(x = forwardOffsetX)
                            .size(56.dp)
                            .graphicsLayer {
                                scaleX = forwardScale
                                scaleY = forwardScale
                            }
                    ) {
                        Icon(
                            imageVector = AppIcons.SkipNext,
                            contentDescription = stringResource(R.string.music_seek_forward_10),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Slider(
                    value = sliderValue,
                    onValueChange = { newValue ->
                        if (!enabled || !isCurrentTrack) return@Slider
                        isSliderDragging = true
                        sliderValue = newValue
                    },
                    onValueChangeFinished = {
                        if (enabled && isCurrentTrack) {
                            MusicPlaybackManager.seekTo(sliderValue.roundToLong())
                        }
                        isSliderDragging = false
                    },
                    valueRange = 0f..sliderMax,
                    enabled = enabled && isCurrentTrack,

                    thumb = {
                        ExpressiveThumb(isDragging = isSliderDragging)
                    },

                    track = { sliderState ->

                        val progress =
                            (sliderState.value - sliderState.valueRange.start) /
                                    (sliderState.valueRange.endInclusive - sliderState.valueRange.start)

                        ExpressiveTrack(
                            progress = progress,
                            isPlaying = isPlaying,
                            isDragging = isSliderDragging
                        )
                    }
                )
            }


            if (footerContent != null) {
                footerContent()
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTimeMillis(currentPositionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTimeMillis(durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistAddersButtonContent(
    adders: List<Author>,
    addersCount: Int,
) {
    val previewAdders = adders.take(2)
    val remainingCount = (addersCount - previewAdders.size).coerceAtLeast(0)
    val label = when {
        remainingCount > 0 -> stringResource(R.string.music_adders_more, remainingCount)
        else -> stringResource(R.string.music_adders_queued, addersCount)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DPMedium)
    ) {
        Box(
            modifier = Modifier.width(44.dp),
        ) {
            previewAdders.forEachIndexed { index, adder ->
                PlaylistAdderAvatar(
                    imageUrl = adder.profile_image_url,
                    badge = adder.badge,
                    profileImageShape = adder.profile_image_shape,
                    modifier = Modifier
                        .offset(x = (index * 14).dp)
                        .size(22.dp)
                )
            }
            if (previewAdders.isEmpty()) {
                Icon(
                    imageVector = AppIcons.Person,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(20.dp)
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlaylistAdderAvatar(
    imageUrl: String?,
    badge: String?,
    profileImageShape: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val normalizedImageUrl = imageUrl?.takeIf { it.isNotBlank() }

    Surface(
        modifier = modifier,
        shape = resolveProfileImageShape(
            badge = badge,
            profileImageShape = profileImageShape
        ),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        if (normalizedImageUrl == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.Person,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Surface
        }

        PlatformSubcomposeAsyncImage(
            model = rememberPlatformImageRequest(
                data = normalizedImageUrl,
                options = PlatformImageRequestOptions(
                    crossfade = true,
                    memoryCacheKey = normalizedImageUrl,
                    enableDiskCache = true,
                    enableMemoryCache = true
                )
            ),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(
                    resolveProfileImageShape(
                        badge = badge,
                        profileImageShape = profileImageShape
                    )
                ),
            error = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Person,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

private fun isNeighborButton(activeButton: Int?, index: Int): Boolean {
    val active = activeButton ?: return false
    return abs(active - index) == 1
}

private fun formatTimeMillis(value: Long): String {
    if (value <= 0L) return "0:00"
    val totalSeconds = value / 1000L
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}


@Composable
fun ExpressiveThumb(isDragging: Boolean) {

    val scale by animateFloatAsState(
        targetValue = if (isDragging) 1.35f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = ""
    )

    Box(
        modifier = Modifier
            .size(14.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(
                MaterialTheme.colorScheme.primary,
                CircleShape
            )
    )
}

@Composable
fun ExpressiveTrack(
    progress: Float,
    isPlaying: Boolean,
    isDragging: Boolean
) {

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val baseLine = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

    val infinite = rememberInfiniteTransition(label = "")

    val waveShift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 9000,
                easing = LinearEasing
            )
        ),
        label = ""
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
    ) {

        val width = size.width
        val height = size.height
        val centerY = height / 2

        val progressX = width * progress

        // اگر pause شود موج صاف می‌شود
        val amplitude =
            if (isPlaying) height * if (isDragging) 1.2f else 0.7f
            else 0f

        val wavelength = 50f   // موج‌های ریزتر

        val path = Path()
        path.moveTo(0f, centerY)

        var x = 0f

        while (x <= progressX) {

            val y =
                centerY + sin((x + waveShift) / wavelength * PI).toFloat() * amplitude

            path.lineTo(x, y)

            x += 6f
        }

        // موج تا قبل از thumb
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(
                listOf(primary, secondary)
            ),
            style = Stroke(
                width = height,
                cap = StrokeCap.Round
            )
        )

        // خط صاف فقط بعد از thumb
        drawLine(
            color = baseLine,
            start = Offset(progressX, centerY),
            end = Offset(width, centerY),
            strokeWidth = height
        )
    }
}
