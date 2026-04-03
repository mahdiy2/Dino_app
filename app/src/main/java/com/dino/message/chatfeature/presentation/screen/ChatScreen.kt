package com.dino.message.chatfeature.presentation.screen

import android.Manifest
import android.content.ClipData
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Badge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.dino.message.R
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dino.message.chatfeature.presentation.component.ChatScreenBottomAppBar
import com.dino.message.chatfeature.presentation.component.ChatSurface
import com.dino.message.chatfeature.presentation.component.MediaAccessLevel
import com.dino.message.chatfeature.presentation.component.MediaPermissionHandler
import com.dino.message.chatfeature.presentation.component.MediaViewerDialog
import com.dino.message.chatfeature.presentation.viewmodel.ChatViewModel
import com.dino.message.profilefeature.presentation.component.ProfileAvatarMedia
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ChatScreen(
    chatId: String,
    displayName: String? = null,
    profileImageUrl: String? = null,
    profileVideoUrl: String? = null
) {
    val viewModel: ChatViewModel = hiltViewModel()
    val isPeerOnline by viewModel.isPeerOnline.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isPublicKeyLoading by viewModel.isPublicKeyLoading.collectAsState()
    val publicKeyError by viewModel.publicKeyError.collectAsState()
    val isPublicKeyReady by viewModel.isPublicKeyReady.collectAsState()
    val socketDebugEvents by viewModel.socketDebugEvents.collectAsState()
    val showFollowRequired by viewModel.showFollowRequired.collectAsState()
    val isFollowLoading by viewModel.isFollowLoading.collectAsState()
    var showDebugLog by remember { mutableStateOf(false) }
    val debugLogCount by remember { derivedStateOf { socketDebugEvents.size } }
    val decryptionError by viewModel.decryptionError.collectAsState()
    val socketError by viewModel.socketError.collectAsState()
    val debugListState = rememberLazyListState()
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingDurationMs by viewModel.recordingDurationMs.collectAsState()
    val replyingTo by viewModel.replyingTo.collectAsState()
    val context = LocalContext.current
    val resolvedName = displayName ?: chatId
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingVoiceAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var mediaViewerUrl by remember { mutableStateOf<String?>(null) }
    val clipboardManager = remember { context.getSystemService(android.content.ClipboardManager::class.java) }

    var mediaViewerIsVideo by remember { mutableStateOf(false) }
    var pendingMediaType by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uploadError) {
        uploadError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissUploadError()
        }
    }

    LaunchedEffect(decryptionError) {
        decryptionError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissDecryptionError()
        }
    }

    LaunchedEffect(socketError) {
        socketError?.let {
            val result = snackbarHostState.showSnackbar(
                message = it,
                actionLabel = "Retry",
                duration = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.retrySocketConnection()
            } else {
                viewModel.dismissSocketError()
            }
        }
    }

    LaunchedEffect(socketDebugEvents.size) {
        if (socketDebugEvents.isNotEmpty()) {
            debugListState.animateScrollToItem(socketDebugEvents.lastIndex)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.sendAttachment(it) }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.sendAttachment(it) }
    }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val accessLevel = MediaPermissionHandler.resolveAccessLevel(permissions)
        if (accessLevel == MediaAccessLevel.DENIED) {
            Toast.makeText(
                context,
                "Media access is required to send photos and videos",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            when (pendingMediaType) {
                "image" -> imagePickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
                "video" -> videoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                )
            }
        }
        pendingMediaType = null
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingVoiceAction?.invoke()
        } else {
            Toast.makeText(
                context,
                "Microphone permission is required to record voice messages",
                Toast.LENGTH_SHORT
            ).show()
        }
        pendingVoiceAction = null
    }

    if (mediaViewerUrl != null) {
        MediaViewerDialog(
            url = mediaViewerUrl!!,
            isVideo = mediaViewerIsVideo,
            onDismiss = { mediaViewerUrl = null }
        )
    }

    if (showFollowRequired) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissFollowRequired() },
            title = {
                Text(text = stringResource(R.string.follow_required_title))
            },
            text = {
                Text(text = stringResource(R.string.follow_required_message))
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.followAndRetry() },
                    enabled = !isFollowLoading
                ) {
                    if (isFollowLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(text = stringResource(R.string.follow))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.dismissFollowRequired() },
                    enabled = !isFollowLoading
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        ProfileAvatarMedia(
                           modifier = Modifier.size(36.dp),
                           profileImageUrl = profileImageUrl,
                            profileVideoUrl = profileVideoUrl,
                            autoPlayVideo = true
                       )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = resolvedName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (isPeerOnline) "Online" else "Offline",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isPeerOnline)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearMessages() }) {
                        Icon(imageVector = Icons.Rounded.Delete, contentDescription = "Clear")
                    }
//                    IconButton(onClick = { showDebugLog = !showDebugLog }) {
//                        BadgedBox(
//                            badge = {
//                                if (debugLogCount > 0) {
//                                    Badge { Text("$debugLogCount") }
//                                }
//                            }
//                        ) {
//                            Icon(
//                                imageVector = Icons.Rounded.BugReport,
//                                contentDescription = "Debug Log"
//                            )
//                        }
//                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateUp() }) {
                        Icon(imageVector = Icons.Rounded.ArrowBackIosNew, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            ChatScreenBottomAppBar(
                onSendClicked = { message ->
                    if (message.isNotBlank()) {
                        viewModel.sendMessage(message)
                    }
                },
                onPickImage = {
                    val currentAccess = MediaPermissionHandler.checkAccessLevel(context)
                    if (currentAccess != MediaAccessLevel.DENIED) {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    } else {
                        pendingMediaType = "image"
                        mediaPermissionLauncher.launch(MediaPermissionHandler.getRequiredPermissions())
                    }
                },
                onPickVideo = {
                    val currentAccess = MediaPermissionHandler.checkAccessLevel(context)
                    if (currentAccess != MediaAccessLevel.DENIED) {
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    } else {
                        pendingMediaType = "video"
                        mediaPermissionLauncher.launch(MediaPermissionHandler.getRequiredPermissions())
                    }
                },
                onVoiceRecordStart = {
                    pendingVoiceAction = { viewModel.startVoiceRecording(context) }
                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                onVoiceRecordStop = { viewModel.stopVoiceRecording() },
                onVoiceRecordCancel = { viewModel.cancelVoiceRecording() },
                isRecording = isRecording,
                recordingDurationMs = recordingDurationMs,
                enabled = isPublicKeyReady,
                replySenderName = replyingTo?.senderOrReceiver,
                replyPreviewText = replyingTo?.content?.take(100),
                onDismissReply = { viewModel.clearReplyingTo() }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    actionColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when {
                isPublicKeyLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                publicKeyError != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = publicKeyError ?: "Failed to load encryption key",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.retryLoadPublicKey() }) {
                            Text("Retry")
                        }
                    }
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (isUploading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize()) {
                                ChatSurface(
                                    modifier = Modifier.fillMaxSize(),
                                    items = messages,
                                    sender = chatId,
                                    onAttachmentClick = { message ->
                                        val url = message.attachmentUrl
                                        if (!url.isNullOrBlank()) {
                                            when (message.messageType) {
                                                "image" -> {
                                                    mediaViewerUrl = url
                                                    mediaViewerIsVideo = false
                                                }
                                                "video" -> {
                                                    mediaViewerUrl = url
                                                    mediaViewerIsVideo = true
                                                }
                                            }
                                        }
                                    },
                                    onMessageLongClick = { message ->
                                        if (message.content.isNotBlank()) {
                                            val clip = ClipData.newPlainText("Message", message.content)
                                            clipboardManager?.setPrimaryClip(clip)
                                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onSwipeToReply = { message ->
                                        viewModel.setReplyingTo(message)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDebugLog) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Socket Debug Log",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row {
                            IconButton(onClick = {
                                val allLogs = socketDebugEvents.joinToString("\n")
                                val clip = ClipData.newPlainText("Socket Debug Log", allLogs)
                                clipboardManager?.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = "Copy All"
                                )
                            }
                            IconButton(onClick = { showDebugLog = false }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close"
                                )
                            }
                        }
                    }

                    if (socketDebugEvents.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No events yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        LazyColumn(
                            state = debugListState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp)
                        ) {
                            items(socketDebugEvents) { event ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = event,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
