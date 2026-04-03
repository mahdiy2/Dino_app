package com.dino.message.groupfeature.presentation.screen

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dino.message.chatfeature.presentation.component.ChatScreenBottomAppBar
import com.dino.message.chatfeature.presentation.component.MediaAccessLevel
import com.dino.message.chatfeature.presentation.component.MediaPermissionHandler
import com.dino.message.chatfeature.presentation.component.MediaViewerDialog
import com.dino.message.groupfeature.presentation.component.GroupChatSurface
import com.dino.message.groupfeature.presentation.viewmodel.GroupChatViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun GroupChatScreen(
    groupId: Int,
    groupName: String? = null,
) {
    val viewModel: GroupChatViewModel = hiltViewModel()
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()
    val socketError by viewModel.socketError.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingDurationMs by viewModel.recordingDurationMs.collectAsState()
    val replyingTo by viewModel.replyingTo.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val resolvedName = groupName ?: viewModel.groupName

    var mediaViewerUrl by remember { mutableStateOf<String?>(null) }
    var mediaViewerIsVideo by remember { mutableStateOf(false) }
    var pendingMediaType by remember { mutableStateOf<String?>(null) }
    var pendingVoiceAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(uploadError) {
        uploadError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissUploadError()
        }
    }
    LaunchedEffect(socketError) {
        socketError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissSocketError()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { viewModel.sendAttachment(it, "image") } }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { viewModel.sendAttachment(it, "video") } }

    val voicePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingVoiceAction?.invoke()
        else Toast.makeText(context, "Microphone permission denied", Toast.LENGTH_SHORT).show()
    }

    if (mediaViewerUrl != null) {
        MediaViewerDialog(
            url = mediaViewerUrl!!,
            isVideo = mediaViewerIsVideo,
            onDismiss = { mediaViewerUrl = null }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(resolvedName, style = MaterialTheme.typography.titleMedium)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateUp() }) {
                        Icon(Icons.Rounded.ArrowBackIosNew, "Back")
                    }
                }
            )
        },
        bottomBar = {
            ChatScreenBottomAppBar(
                onSendClicked = { viewModel.sendTextMessage(it) },
                onPickImage = {
                    if (MediaPermissionHandler.checkAccessLevel(context) != MediaAccessLevel.DENIED) {
                        imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    } else {
                        pendingMediaType = "image"
                    }
                },
                onPickVideo = {
                    if (MediaPermissionHandler.checkAccessLevel(context) != MediaAccessLevel.DENIED) {
                        videoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    } else {
                        pendingMediaType = "video"
                    }
                },
                onVoiceRecordStart = {
                    val action = { viewModel.startRecording(context) }
                    if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        action()
                    } else {
                        pendingVoiceAction = action
                        voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onVoiceRecordStop = { viewModel.stopRecordingAndSend() },
                onVoiceRecordCancel = { viewModel.cancelRecording() },
                isRecording = isRecording,
                recordingDurationMs = recordingDurationMs,
                enabled = !isUploading,
                replySenderName = replyingTo?.sender,
                replyPreviewText = replyingTo?.content,
                onDismissReply = { viewModel.clearReply() }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                isUploading -> {
                    Column(Modifier.fillMaxSize()) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Box(modifier = Modifier.fillMaxSize()) {
                            GroupChatSurface(
                                items = messages,
                                currentUsername = viewModel.getMyUsername(),
                                onAttachmentClick = { msg ->
                                    val url = msg.attachmentUrl
                                    if (!url.isNullOrBlank()) {
                                        when (msg.messageType) {
                                            "image" -> { mediaViewerUrl = url; mediaViewerIsVideo = false }
                                            "video" -> { mediaViewerUrl = url; mediaViewerIsVideo = true }
                                        }
                                    }
                                },
                                onMessageLongClick = { msg ->
                                    if (msg.content.isNotBlank()) {
                                        val clip = android.content.ClipData.newPlainText("Message", msg.content)
                                        context.getSystemService(android.content.ClipboardManager::class.java)
                                            ?.setPrimaryClip(clip)
                                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onSwipeToReply = { viewModel.setReplyingTo(it) }
                            )
                        }
                    }
                }

                else -> GroupChatSurface(
                    modifier = Modifier.fillMaxSize(),
                    items = messages,
                    currentUsername = viewModel.getMyUsername(),
                    onAttachmentClick = { msg ->
                        val url = msg.attachmentUrl
                        if (!url.isNullOrBlank()) {
                            when (msg.messageType) {
                                "image" -> { mediaViewerUrl = url; mediaViewerIsVideo = false }
                                "video" -> { mediaViewerUrl = url; mediaViewerIsVideo = true }
                            }
                        }
                    },
                    onMessageLongClick = { msg ->
                        if (msg.content.isNotBlank()) {
                            val clip = android.content.ClipData.newPlainText("Message", msg.content)
                            context.getSystemService(android.content.ClipboardManager::class.java)
                                ?.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSwipeToReply = { viewModel.setReplyingTo(it) }
                )
            }
        }
    }
}
