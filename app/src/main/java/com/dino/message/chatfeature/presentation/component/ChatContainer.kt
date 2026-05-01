package com.dino.message.chatfeature.presentation.component

import android.media.MediaPlayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dino.message.chatfeature.domain.model.MessageDeliveryStatus
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.corefeature.presentation.util.decodePostShareMessage
import com.dino.message.corefeature.presentation.util.rememberLayoutDirection
import com.dino.message.threadfeature.presentation.music.MusicPlaybackManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatContainer(
    text: String,
    sent: Boolean = true,
    messageType: String = "text",
    attachmentType: String? = null,
    attachmentUrl: String? = null,
    attachmentMimeType: String? = null,
    attachmentFileName: String? = null,
    deliveryStatus: MessageDeliveryStatus? = null,
    onClick: ((IntOffset, IntSize) -> Unit)? = null,
    onAttachmentClick: (() -> Unit)? = null,
    onPostShareClick: ((Long) -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    replySenderName: String? = null,
    replyPreviewText: String? = null
) {
    val normalizedAttachmentType = attachmentType?.trim()?.lowercase()
    val isStoryReferenceAttachment =
        normalizedAttachmentType == "story_reply" || normalizedAttachmentType == "story_mention"
    val bubbleShape = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = if (sent) 20.dp else 4.dp,
        bottomEnd = if (sent) 4.dp else 20.dp
    )

    Card(
        shape = bubbleShape,
        colors = CardDefaults.cardColors(
            containerColor = if (sent)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (sent)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        var bubbleOffset = IntOffset.Zero
        var bubbleSize = IntSize.Zero
        Column(
            modifier = Modifier
                .padding(DPMedium)
                .widthIn(max = 280.dp)
                .onGloballyPositioned { coordinates ->
                    val position = coordinates.positionInRoot()
                    bubbleOffset = IntOffset(position.x.toInt(), position.y.toInt())
                    bubbleSize = coordinates.size
                }
                .combinedClickable(
                    onClick = { onClick?.invoke(bubbleOffset, bubbleSize) },
                    onLongClick = onLongClick,
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
               )
        ) {
            val postSharePayload = remember(text) { decodePostShareMessage(text) }
            val effectiveMessageType = if (messageType == "post" || postSharePayload != null) {
                "post"
            } else {
                messageType
            }
            val hasReply = !replySenderName.isNullOrBlank() && replySenderName != "null" &&
                    !replyPreviewText.isNullOrBlank() && replyPreviewText != "null"
            if (hasReply) {
                Row(
                   modifier = Modifier
                       .fillMaxWidth()
                       .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (sent)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
                        )
                        .padding(DPMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(28.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(2.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(DPMedium))
                    Column {
                        Text(
                            text = replySenderName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = replyPreviewText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (sent)
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.height(DPSmall))
            }
            if (isStoryReferenceAttachment && !attachmentUrl.isNullOrBlank()) {
                StoryReferencePreviewCard(
                    mediaUrl = attachmentUrl,
                    mediaType = attachmentMimeType,
                    onClick = onAttachmentClick
                )
                Spacer(modifier = Modifier.height(DPSmall))
            }
            val showStatus = sent && deliveryStatus != null
            if (effectiveMessageType == "text" && text.isNotBlank()) {
                TextWithDeliveryStatus(
                    text = text,
                    sent = sent,
                    deliveryStatus = deliveryStatus,
                )
            } else {
                when (effectiveMessageType) {
                    "image" -> ImageBubble(
                        attachmentUrl = attachmentUrl,
                        attachmentFileName = attachmentFileName,
                        text = text,
                        onClick = onAttachmentClick
                    )

                    "video" -> VideoBubble(
                        attachmentUrl = attachmentUrl,
                        attachmentFileName = attachmentFileName,
                        text = text,
                        onClick = onAttachmentClick
                    )

                    "voice" -> VoiceBubble(
                        attachmentUrl = attachmentUrl,
                        sent = sent
                    )

                    "post" -> PostShareBubble(
                        rawText = text,
                        payload = postSharePayload,
                        onClick = onPostShareClick
                    )

                    else -> {
                        if (text.isNotBlank()) {
                            CompositionLocalProvider(LocalLayoutDirection provides rememberLayoutDirection(text)) {
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
            if (sent && effectiveMessageType != "text") {
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.align(Alignment.End)) {
                    Box(
                        modifier = Modifier.width(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (showStatus) {
                            DeliveryStatusIcon(deliveryStatus = deliveryStatus)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StoryReferencePreviewCard(
    mediaUrl: String,
    mediaType: String?,
    onClick: (() -> Unit)?
) {
    val isVideo = mediaType?.contains("video", ignoreCase = true) == true
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(mediaUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Story preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            if (isVideo) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Text(
                text = "Story reply",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun TextWithDeliveryStatus(
    text: String,
    sent: Boolean,
    deliveryStatus: MessageDeliveryStatus?,
) {
    if (!sent) {
        CompositionLocalProvider(LocalLayoutDirection provides rememberLayoutDirection(text)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    val textDirection = rememberLayoutDirection(text)
    val statusReservedWidth = if (deliveryStatus != null) 22.dp else 0.dp
    CompositionLocalProvider(LocalLayoutDirection provides textDirection) {
        Box {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(end = statusReservedWidth)
            )
            if (deliveryStatus != null) {
                DeliveryStatusIcon(
                    deliveryStatus = deliveryStatus,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
    }
}

@Composable
private fun PostShareBubble(
    rawText: String,
    payload: com.dino.message.corefeature.presentation.util.PostSharePayload?,
    onClick: ((Long) -> Unit)?,
) {
    if (payload == null) {
        if (rawText.isNotBlank()) {
            Text(
                text = rawText,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick?.invoke(payload.postId) },
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
        tonalElevation = 1.dp,
        shadowElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(DPMedium)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Forum,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Shared post from @${payload.authorUsername}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (payload.previewText.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = payload.previewText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tap to open post",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DeliveryStatusIcon(
    deliveryStatus: MessageDeliveryStatus,
    modifier: Modifier = Modifier
) {
    val isDelivered = deliveryStatus == MessageDeliveryStatus.DELIVERED
    val isSeen = deliveryStatus == MessageDeliveryStatus.SEEN
    val isFailed = deliveryStatus == MessageDeliveryStatus.FAILED
    Icon(
        imageVector = if (isFailed) {
            Icons.Rounded.ErrorOutline
        } else if (isDelivered || isSeen) {
            Icons.Rounded.DoneAll
        } else {
            Icons.Rounded.Done
        },
        contentDescription = if (isFailed) {
            "Failed"
        } else if (isSeen) {
            "Seen"
        } else if (isDelivered) {
            "Delivered"
        } else {
            "Sent"
        },
        modifier = modifier.size(14.dp),
        tint = if (isFailed) {
            MaterialTheme.colorScheme.error
        } else if (isSeen) {
            MaterialTheme.colorScheme.primary
        } else if (isDelivered) {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
        }
    )
}

@Composable
private fun ImageBubble(
    attachmentUrl: String?,
    attachmentFileName: String?,
    text: String,
    onClick: (() -> Unit)?
) {
    if (!attachmentUrl.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(attachmentUrl)
                .crossfade(true)
                .build(),
            contentDescription = attachmentFileName ?: "Image",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick?.invoke() },
            contentScale = ContentScale.Crop
        )
    }
    if (text.isNotBlank()) {
        Spacer(modifier = Modifier.height(DPSmall))
        CompositionLocalProvider(LocalLayoutDirection provides rememberLayoutDirection(text)) {
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun VideoBubble(
    attachmentUrl: String?,
    attachmentFileName: String?,
    text: String,
    onClick: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick?.invoke() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play video",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(DPMedium))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = attachmentFileName ?: "Video",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
    if (text.isNotBlank()) {
        Spacer(modifier = Modifier.height(DPSmall))
        CompositionLocalProvider(LocalLayoutDirection provides rememberLayoutDirection(text)) {
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun VoiceBubble(
    attachmentUrl: String?,
    sent: Boolean
) {
    if (attachmentUrl.isNullOrBlank()) return
    val context = LocalContext.current

    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var durationMs by remember { mutableStateOf(0) }
    var currentMs by remember { mutableStateOf(0) }
    val mediaPlayer = remember { MediaPlayer() }
    var isPrepared by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }

    DisposableEffect(attachmentUrl) {
        try {
            mediaPlayer.reset()
            mediaPlayer.setDataSource(attachmentUrl)
            mediaPlayer.setOnPreparedListener {
                durationMs = it.duration
                isPrepared = true
            }
            mediaPlayer.setOnCompletionListener {
                isPlaying = false
                progress = 0f
                currentMs = 0
            }
            mediaPlayer.setOnErrorListener { _, _, _ ->
                hasError = true
                isPlaying = false
                true
            }
            mediaPlayer.prepareAsync()
        } catch (_: Exception) {
            hasError = true
        }
        onDispose {
            try {
                mediaPlayer.release()
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying && isPrepared) {
            try {
                currentMs = mediaPlayer.currentPosition
                progress = if (durationMs > 0) currentMs.toFloat() / durationMs else 0f
            } catch (_: Exception) { }
            delay(100)
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label = "voice_progress"
    )

    val totalSec = durationMs / 1000
    val curSec = currentMs / 1000
    val displaySec = if (isPlaying) curSec else totalSec
    val timeLabel = "%d:%02d".format(displaySec / 60, displaySec % 60)

    val accentColor = if (sent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.widthIn(min = 180.dp)
    ) {
        IconButton(
            onClick = {
                if (hasError || !isPrepared) return@IconButton
                if (isPlaying) {
                    mediaPlayer.pause()
                    isPlaying = false
                } else {
                    MusicPlaybackManager.pauseForCompetingAudio(context)
                    mediaPlayer.start()
                    isPlaying = true
                }
            },
            modifier = Modifier.size(40.dp),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = accentColor
            )
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(DPMedium))

        Column(modifier = Modifier.weight(1f)) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (hasError) "Error" else timeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
