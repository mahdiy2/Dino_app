package com.dino.message.threadfeature.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.dino.message.corefeature.presentation.platform.PlatformAsyncImage
import com.dino.message.corefeature.presentation.platform.PlatformVideoPlayer
import com.dino.message.corefeature.presentation.platform.PlatformVideoResizeMode
import com.dino.message.profilefeature.presentation.component.ProfileAvatarMedia
import com.dino.message.threadfeature.domain.model.StoryBundle
import com.dino.message.threadfeature.domain.model.StoryViewer
import kotlinx.coroutines.delay

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun StoryViewerOverlay(
    bundle: StoryBundle,
    viewerUsername: String,
    viewers: List<StoryViewer>,
    isActionInFlight: Boolean,
    showInsights: Boolean,
    isInsightsLoading: Boolean,
    onDismiss: () -> Unit,
    onStoryShown: (Long) -> Unit,
    onLikeStory: (Long, Boolean) -> Unit,
    onReplyStory: (Long, String) -> Unit,
    onOpenChatWithOwner: (String) -> Unit,
    onOpenNextStoryBundle: () -> Unit,
    onOpenPreviousStoryBundle: () -> Unit,
    onDeleteStory: (Long) -> Unit,
    onLoadInsights: (Long) -> Unit,
    onDismissInsights: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    val stories = bundle.user_stories
    if (stories.isEmpty()) return@CompositionLocalProvider

    val initialIndex = stories.indexOfFirst { it.story_id == bundle.story.story_id }.coerceAtLeast(0)
    var currentIndex by rememberSaveable(bundle.owner.user_id) { mutableIntStateOf(initialIndex) }
    val current = stories.getOrNull(currentIndex) ?: stories.first()
    var holdToPause by remember { mutableStateOf(false) }
    var manualPause by remember(currentIndex) { mutableStateOf(false) }
    var progress by remember(currentIndex) { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var replyDraft by remember { mutableStateOf("") }
    val replyInteractionSource = remember { MutableInteractionSource() }
    val isReplyFieldFocused by replyInteractionSource.collectIsFocusedAsState()

    val isVideoStory = current.media_type.equals("video", ignoreCase = true)
    val isOwnerViewing = bundle.owner.username == viewerUsername
    val isLiked = current.viewer_liked
    val isAutoAdvancePaused = manualPause || holdToPause || isReplyFieldFocused || showInsights || isInsightsLoading
    var videoProgress by remember(current.story_id) { mutableFloatStateOf(0f) }
    var videoStarted by remember(current.story_id) { mutableStateOf(false) }
    var videoAdvanceHandled by remember(current.story_id) { mutableStateOf(false) }
    var videoDurationMillis by remember(current.story_id) { mutableLongStateOf(0L) }
    var isVideoBuffering by remember(current.story_id) { mutableStateOf(isVideoStory) }
    var videoPlaybackError by remember(current.story_id) { mutableStateOf<String?>(null) }

    val advanceToNextStory = {
        if (currentIndex < stories.lastIndex) {
            currentIndex += 1
        } else {
            onOpenNextStoryBundle()
        }
    }

    val retreatToPreviousStory = {
        if (currentIndex > 0) {
            currentIndex -= 1
        } else {
            onOpenPreviousStoryBundle()
        }
    }

    LaunchedEffect(current.story_id) {
        onStoryShown(current.story_id)
        progress = 0f
        manualPause = false
        videoProgress = 0f
        videoStarted = false
        videoAdvanceHandled = false
        videoDurationMillis = 0L
        isVideoBuffering = isVideoStory
        videoPlaybackError = null
    }

    LaunchedEffect(current.story_id, videoPlaybackError, videoAdvanceHandled) {
        if (!isVideoStory || videoPlaybackError.isNullOrBlank() || !videoAdvanceHandled) return@LaunchedEffect
        delay(1400L)
        advanceToNextStory()
    }

    LaunchedEffect(current.story_id, isAutoAdvancePaused, isVideoStory) {
        if (isAutoAdvancePaused || isVideoStory) return@LaunchedEffect
        val durationMillis = 5000L
        val stepMillis = 100L
        while (progress < 1f) {
            delay(stepMillis)
            progress += stepMillis.toFloat() / durationMillis.toFloat()
        }
        if (progress >= 1f) {
            advanceToNextStory()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.96f))
            .pointerInput(currentIndex) {
                detectDragGestures(
                    onDragEnd = {
                        if (dragOffsetY > 220f) {
                            onDismiss()
                        }
                        dragOffsetY = 0f
                    }
                ) { _, dragAmount ->
                    if (dragAmount.y > 0f) {
                        dragOffsetY += dragAmount.y
                    }
                }
            }
            .padding(top = 22.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 24.dp)
        ) {
            if (!isVideoStory) {
                PlatformAsyncImage(
                    model = current.media_url,
                    contentDescription = "Story",
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha((1f - (dragOffsetY / 460f)).coerceIn(0.6f, 1f)),
                    contentScale = ContentScale.Fit
                )
            } else {
                StoryVideoPlayer(
                    mediaUrl = current.media_url,
                    isPaused = isAutoAdvancePaused,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha((1f - (dragOffsetY / 460f)).coerceIn(0.6f, 1f)),
                    onBufferingChanged = { buffering ->
                        isVideoBuffering = buffering
                    },
                    onDurationKnown = { durationMillis ->
                        if (durationMillis > 0) {
                            videoDurationMillis = durationMillis
                        }
                    },
                    onStarted = {
                        videoStarted = true
                    },
                    onProgress = { currentMillis ->
                        val duration = videoDurationMillis
                        if (duration > 0L) {
                            videoProgress = (currentMillis.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                        }
                    },
                    onCompleted = {
                        if (!videoAdvanceHandled) {
                            videoAdvanceHandled = true
                            videoProgress = 1f
                            advanceToNextStory()
                        }
                    },
                    onPlaybackError = {
                        if (!videoAdvanceHandled) {
                            videoAdvanceHandled = true
                            videoPlaybackError = "Couldn't play this video. Skipping to next story."
                        }
                    }
                )

                if (isVideoBuffering && !videoStarted) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        }
                    }
                } else if (!videoStarted) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (!videoPlaybackError.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f)
                    ) {
                        Text(
                            text = videoPlaybackError.orEmpty(),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(current.story_id, currentIndex) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val up = waitForUpOrCancellation()
                                val tapDistance = up?.let { (it.position - down.position).getDistance() } ?: Float.MAX_VALUE
                                val pressDuration = up?.let { it.uptimeMillis - down.uptimeMillis } ?: Long.MAX_VALUE
                                if (up != null && tapDistance < 24f && pressDuration < 260L) {
                                    retreatToPreviousStory()
                                }
                            }
                        }
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(current.story_id, manualPause) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                holdToPause = true
                                val up = waitForUpOrCancellation()
                                holdToPause = false
                                if (up != null) {
                                    val pressDuration = up.uptimeMillis - down.uptimeMillis
                                    if (pressDuration < 220L) {
                                        manualPause = !manualPause
                                    }
                                }
                            }
                        }
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .pointerInput(current.story_id, currentIndex) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val up = waitForUpOrCancellation()
                                val tapDistance = up?.let { (it.position - down.position).getDistance() } ?: Float.MAX_VALUE
                                val pressDuration = up?.let { it.uptimeMillis - down.uptimeMillis } ?: Long.MAX_VALUE
                                if (up != null && tapDistance < 24f && pressDuration < 260L) {
                                    advanceToNextStory()
                                }
                            }
                        }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                stories.forEachIndexed { index, _ ->
                    val value = when {
                        index < currentIndex -> 1f
                        index == currentIndex -> {
                            if (isVideoStory) {
                                videoProgress.coerceIn(0f, 1f)
                            } else {
                                progress.coerceIn(0f, 1f)
                            }
                        }
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = { value },
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.18f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = bundle.owner.username.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = bundle.owner.username,
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 18.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            if (!isOwnerViewing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { onLikeStory(current.story_id, !isLiked) },
                        enabled = !isActionInFlight
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (isLiked) Color(0xFFFF6B7A) else Color.White
                        )
                    }

                    OutlinedTextField(
                        value = replyDraft,
                        onValueChange = { replyDraft = it },
                        placeholder = {
                            Text(
                                text = "Reply to story",
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = !isActionInFlight,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        shape = CircleShape,
                        interactionSource = replyInteractionSource
                    )

                    IconButton(
                        onClick = {
                            val text = replyDraft.trim()
                            if (text.isNotEmpty()) {
                                onReplyStory(current.story_id, text)
                                replyDraft = ""
                            } else {
                                onOpenChatWithOwner(bundle.owner.username)
                            }
                        },
                        enabled = !isActionInFlight
                    ) {
                        if (isActionInFlight) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = "Send reply",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            if (isOwnerViewing) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = { onDeleteStory(current.story_id) },
                        enabled = !isActionInFlight
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Delete story",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { onLoadInsights(current.story_id) }) {
                        Icon(
                            imageVector = Icons.Rounded.BarChart,
                            contentDescription = "Viewers",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    if (showInsights) {
        ModalBottomSheet(onDismissRequest = onDismissInsights) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Story viewers",
                    style = MaterialTheme.typography.titleMedium
                )

                if (isInsightsLoading) {
                    CircularProgressIndicator()
                } else if (viewers.isEmpty()) {
                    Text(
                        text = "No viewers yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    viewers.forEach { viewer ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProfileAvatarMedia(
                                modifier = Modifier.size(32.dp),
                                profileImageUrl = viewer.avatar,
                                profileVideoUrl = null,
                                badge = viewer.badge,
                                profileImageShape = viewer.profile_image_shape
                            )
                            Text(
                                text = viewer.username,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (viewer.liked) "Liked" else "Viewed",
                                color = if (viewer.liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun StoryVideoPlayer(
    mediaUrl: String,
    isPaused: Boolean,
    onBufferingChanged: (Boolean) -> Unit,
    onDurationKnown: (Long) -> Unit,
    onStarted: () -> Unit,
    onProgress: (Long) -> Unit,
    onCompleted: () -> Unit,
    onPlaybackError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    PlatformVideoPlayer(
        url = mediaUrl,
        modifier = modifier,
        useController = false,
        resizeMode = PlatformVideoResizeMode.Fit,
        isPaused = isPaused,
        playWhenReady = !isPaused,
        pauseCompetingAudio = false,
        transparentShutter = true,
        onBufferingChanged = onBufferingChanged,
        onDurationKnown = onDurationKnown,
        onStarted = onStarted,
        onProgress = onProgress,
        onCompleted = onCompleted,
        onPlaybackError = onPlaybackError
    )
}
