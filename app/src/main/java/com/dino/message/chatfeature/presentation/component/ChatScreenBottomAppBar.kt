package com.dino.message.chatfeature.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.dino.message.R
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.corefeature.presentation.util.rememberLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge

@Composable
fun ChatScreenBottomAppBar(
    modifier: Modifier = Modifier,
    onSendClicked: (String) -> Unit,
    onInputTextChanged: (String) -> Unit = {},
    onPickImage: () -> Unit = {},
    onPickVideo: () -> Unit = {},
    onVoiceRecordStart: () -> Unit = {},
    onVoiceRecordStop: () -> Unit = {},
    onVoiceRecordCancel: () -> Unit = {},
    isRecording: Boolean = false,
    recordingDurationMs: Long = 0L,
    enabled: Boolean = true,
    replySenderName: String? = null,
    replyPreviewText: String? = null,
    onDismissReply: () -> Unit = {}
) {
    val value = remember { mutableStateOf("") }
    var showAttachMenu by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        AnimatedVisibility(
            visible = replySenderName != null && replyPreviewText != null,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            ReplyPreviewBanner(
                senderName = replySenderName ?: "",
                previewText = replyPreviewText ?: "",
                onDismiss = onDismissReply
            )
        }
        BottomAppBar(
            containerColor = Color.Transparent,
            modifier = Modifier
                .padding(horizontal = DPMedium)
            // todo : change it later if you need
//                .height(80.dp)
//                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                AnimatedContent(
                    targetState = isRecording,
                    label = "bottom_bar_mode"
                ) { recording ->
                    if (recording) {
                        RecordingBar(
                            durationMs = recordingDurationMs,
                            onStop = onVoiceRecordStop,
                            onCancel = onVoiceRecordCancel
                        )
                    } else {
                        InputBar(
                            text = value.value,
                            onTextChange = {
                                value.value = it
                                onInputTextChanged(it)
                            },
                            onSend = {
                                onSendClicked(value.value)
                                value.value = ""
                            },
                            showAttachMenu = showAttachMenu,
                            onToggleAttachMenu = { showAttachMenu = !showAttachMenu },
                            onDismissAttachMenu = { showAttachMenu = false },
                            onPickImage = {
                                showAttachMenu = false
                                onPickImage()
                            },
                            onPickVideo = {
                                showAttachMenu = false
                                onPickVideo()
                            },
                            onMicClick = onVoiceRecordStart,
                            enabled = enabled
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    showAttachMenu: Boolean,
    onToggleAttachMenu: () -> Unit,
    onDismissAttachMenu: () -> Unit,
    onPickImage: () -> Unit,
    onPickVideo: () -> Unit,
    onMicClick: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {

        val rotation by animateFloatAsState(
            targetValue = if (showAttachMenu) 45f else 0f,
            animationSpec = tween(220, easing = FastOutSlowInEasing),
            label = "attach_icon_rotation"
        )

        val scale by animateFloatAsState(
            targetValue = if (showAttachMenu) 1.1f else 1f,
            animationSpec = tween(220, easing = FastOutSlowInEasing),
            label = "attach_icon_scale"
        )
        Box {
            IconButton(
                modifier = Modifier
                    .size(44.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                onClick = onToggleAttachMenu,
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.attach),
                    modifier = Modifier
                        .rotate(rotation)
                        .scale(scale),
                    tint = if (enabled)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.38f)
                )
            }
            DropdownMenu(
                shape = MaterialTheme.shapes.extraLarge,
                expanded = showAttachMenu,
                onDismissRequest = onDismissAttachMenu
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.photo)) },
                    onClick = onPickImage,
                    leadingIcon = {
                        Icon(Icons.Default.Image, contentDescription = null)
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.video)) },
                    onClick = onPickVideo,
                    leadingIcon = {
                        Icon(Icons.Default.Videocam, contentDescription = null)
                    }
                )
            }
        }

        Spacer(Modifier.size(DPMedium))

        CompositionLocalProvider(LocalLayoutDirection provides rememberLayoutDirection(text)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    enabled = enabled,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSecondaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.message),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        Spacer(Modifier.size(DPMedium))



        AnimatedContent(
            targetState = text.isNotBlank(),
            label = "send_mic_animation",
            transitionSpec = {
                fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
            }
        ) { hasText ->
            if (hasText) {
                IconButton(
                    modifier = Modifier.size(44.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    enabled = enabled,
                    onClick = onSend
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_send_filled),
                        contentDescription = stringResource(R.string.send),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            } else {
                IconButton(
                    modifier = Modifier.size(44.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    enabled = enabled,
                    onClick = onMicClick
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = stringResource(R.string.record_voice),
                        tint = if (enabled)
                            MaterialTheme.colorScheme.onSecondaryContainer
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingBar(
    durationMs: Long,
    onStop: () -> Unit,
    onCancel: () -> Unit
) {
    val totalSeconds = (durationMs / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val timeText = "%d:%02d".format(minutes, seconds)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DPMedium),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onCancel) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.cancel_recording),
                tint = MaterialTheme.colorScheme.error
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .scale(pulseScale)
                    .background(
                        color = MaterialTheme.colorScheme.error,
                        shape = CircleShape
                    )
            )
            Spacer(Modifier.width(DPMedium))
            Text(
                text = timeText,
                style = MaterialTheme.typography.titleMedium
            )
        }

        IconButton(
            modifier = Modifier
                .size(44.dp),
            onClick = onStop,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_send_filled),
                contentDescription = stringResource(R.string.send_voice),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
