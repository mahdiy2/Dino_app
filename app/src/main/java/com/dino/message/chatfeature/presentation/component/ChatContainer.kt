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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.corefeature.presentation.util.rememberLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatContainer(
    text: String,
    sent: Boolean = true,
    messageType: String = "text",
    attachmentUrl: String? = null,
    attachmentMimeType: String? = null,
    attachmentFileName: String? = null,
    onAttachmentClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
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
        Column(
            modifier = Modifier
                .padding(DPMedium)
                .widthIn(max = 280.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick,
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                )
        ) {
            when (messageType) {
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
    }
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
