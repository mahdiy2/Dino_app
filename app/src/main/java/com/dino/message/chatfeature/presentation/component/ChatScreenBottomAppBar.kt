package com.dino.message.chatfeature.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall

@Composable
fun ChatScreenBottomAppBar(
    onSendClicked: (String) -> Unit,
    onPickImage: () -> Unit = {},
    onPickVideo: () -> Unit = {},
    onVoiceRecordStart: () -> Unit = {},
    onVoiceRecordStop: () -> Unit = {},
    onVoiceRecordCancel: () -> Unit = {},
    isRecording: Boolean = false,
    recordingDurationMs: Long = 0L,
    enabled: Boolean = true
) {
    val value = remember { mutableStateOf("") }
    var showAttachMenu by remember { mutableStateOf(false) }

    BottomAppBar {
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
                    onTextChange = { value.value = it },
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
        Box {
            IconButton(
                onClick = onToggleAttachMenu,
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Attach",
                    tint = if (enabled)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
            DropdownMenu(
                expanded = showAttachMenu,
                onDismissRequest = onDismissAttachMenu
            ) {
                DropdownMenuItem(
                    text = { Text("Photo") },
                    onClick = onPickImage,
                    leadingIcon = {
                        Icon(Icons.Default.Image, contentDescription = null)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Video") },
                    onClick = onPickVideo,
                    leadingIcon = {
                        Icon(Icons.Default.Videocam, contentDescription = null)
                    }
                )
            }
        }

        Spacer(Modifier.size(DPSmall))

        OutlinedTextField(
            modifier = Modifier
                .weight(1f)
                .padding(end = DPSmall),
            enabled = enabled,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = MaterialTheme.colorScheme.inversePrimary,
                focusedContainerColor = MaterialTheme.colorScheme.inversePrimary,
                cursorColor = Color.White,
                focusedPlaceholderColor = Color.Transparent,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.secondary
            ),
            shape = MaterialTheme.shapes.extraLarge,
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("Message") },
            singleLine = true
        )

        if (text.isNotBlank()) {
            IconButton(
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = enabled,
                onClick = onSend
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        } else {
            IconButton(
                enabled = enabled,
                onClick = onMicClick
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Record voice",
                    tint = if (enabled)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
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
                contentDescription = "Cancel recording",
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
            onClick = onStop,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send voice",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
