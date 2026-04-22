package com.dino.message.groupfeature.presentation.screen

import android.Manifest
import android.content.ClipData
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.dino.message.chatfeature.presentation.component.ChatScreenBottomAppBar
import com.dino.message.chatfeature.presentation.component.MediaAccessLevel
import com.dino.message.chatfeature.presentation.component.MediaPermissionHandler
import com.dino.message.chatfeature.presentation.component.MediaViewerDialog
import com.dino.message.groupfeature.domain.model.GroupMessageEntity
import com.dino.message.groupfeature.presentation.component.GroupChatSurface
import com.dino.message.groupfeature.presentation.viewmodel.GroupChatViewModel
import com.dino.message.threadfeature.presentation.component.NowPlayingTopBarRow
import com.dino.message.threadfeature.presentation.music.MusicPlaybackManager
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.annotation.parameters.DeepLink
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(
    deepLinks = [
        DeepLink(uriPattern = "secmessage://group/{groupId}")
    ]
)
@Composable
fun GroupChatScreen(
    groupId: Int,
    groupName: String? = null,
) {
    val viewModel: GroupChatViewModel = hiltViewModel()
    val messages by viewModel.messages.collectAsState()
    val messageDeliveryStatuses by viewModel.messageDeliveryStatuses.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingOlderHistory by viewModel.isLoadingOlderHistory.collectAsState()
    val canLoadOlderHistory by viewModel.canLoadOlderHistory.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()
    val socketError by viewModel.socketError.collectAsState()
    val exitChatReason by viewModel.exitChatReason.collectAsState()
    val decryptionError by viewModel.decryptionError.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingDurationMs by viewModel.recordingDurationMs.collectAsState()
    val replyingTo by viewModel.replyingTo.collectAsState()
    val memberProfileImageUrls by viewModel.memberProfileImageUrls.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val resolvedName = groupName ?: viewModel.groupName
    val myUsername = viewModel.getMyUsername()
    val playbackState by MusicPlaybackManager.playbackState.collectAsState()
    val hasNowPlaying = !playbackState.currentTrackUrl.isNullOrBlank()
    var nowPlayingHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val nowPlayingHeightDp = with(density) { nowPlayingHeightPx.toDp() }
    val reservedNowPlayingHeight = if (!hasNowPlaying) 0.dp else nowPlayingHeightDp.takeIf { it > 0.dp } ?: 64.dp
    val groupListState = rememberLazyListState()

    var mediaViewerUrl by remember { mutableStateOf<String?>(null) }
    var mediaViewerIsVideo by remember { mutableStateOf(false) }
    var pendingMediaType by remember { mutableStateOf<String?>(null) }
    var pendingVoiceAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var selectedMessage by remember { mutableStateOf<GroupMessageEntity?>(null) }
    var selectedMessageAnchor by remember { mutableStateOf<Pair<IntOffset, IntSize>?>(null) }
    var deleteCandidate by remember { mutableStateOf<GroupMessageEntity?>(null) }

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
    LaunchedEffect(exitChatReason) {
        exitChatReason?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.consumeExitChatReason()
            viewModel.navigateUp()
        }
    }
    LaunchedEffect(decryptionError) {
        decryptionError?.let {
            val result = snackbarHostState.showSnackbar(
                message = it,
                actionLabel = "Change key",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.openPrivateKeyResetDialog()
            }
            viewModel.dismissDecryptionError()
        }
    }
    LaunchedEffect(hasNowPlaying) {
        if (!hasNowPlaying) nowPlayingHeightPx = 0
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { viewModel.sendAttachment(it, "image") } }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { viewModel.sendAttachment(it, "video") } }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
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

    if (selectedMessage != null && selectedMessageAnchor != null) {
        val message = selectedMessage!!
        val (anchorOffset, anchorSize) = selectedMessageAnchor!!
        val density = LocalDensity.current
        val menuHeightPx = with(density) { 132.dp.roundToPx() }
        val verticalGapPx = with(density) { 8.dp.roundToPx() }
        val popupOffset = IntOffset(
            x = anchorOffset.x + (anchorSize.width / 2),
            y = (anchorOffset.y - menuHeightPx - verticalGapPx).coerceAtLeast(0)
        )

        Popup(
            alignment = Alignment.TopStart,
            offset = popupOffset,
            onDismissRequest = {
                selectedMessage = null
                selectedMessageAnchor = null
            },
            properties = PopupProperties(focusable = true)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 10.dp,
                shadowElevation = 10.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Column(
                    modifier = Modifier
                        .width(210.dp)
                        .padding(vertical = 6.dp)
                ) {
                    GroupMessageMenuRow(
                        icon = Icons.Rounded.AccessTime,
                        title = formatMessageSentAt(message.timestamp),
                        onClick = {},
                        enabled = false
                    )
                    GroupMessageMenuRow(
                        icon = Icons.Rounded.ContentCopy,
                        title = "Copy",
                        onClick = {
                            if (message.content.isNotBlank()) {
                                val clip = ClipData.newPlainText("Message", message.content)
                                context.getSystemService(android.content.ClipboardManager::class.java)
                                    ?.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Nothing to copy", Toast.LENGTH_SHORT).show()
                            }
                            selectedMessage = null
                            selectedMessageAnchor = null
                        }
                    )
                    GroupMessageMenuRow(
                        icon = Icons.Rounded.Delete,
                        title = "Delete",
                        onClick = {
                            deleteCandidate = message
                            selectedMessage = null
                            selectedMessageAnchor = null
                        }
                    )
                }
            }
        }
    }

    if (deleteCandidate != null) {
        val message = deleteCandidate!!
        val isMine = message.sender == myUsername
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete message") },
            text = {
                Text(
                    if (isMine) {
                        "Delete this message only for you, or for everyone in the group?"
                    } else {
                        "Delete this message only for you?"
                    }
                )
            },
            confirmButton = {
                if (isMine) {
                    TextButton(
                        onClick = {
                            viewModel.deleteMessageForEveryone(message)
                            deleteCandidate = null
                        }
                    ) { Text("For everyone") }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMessageOnlyForMe(message)
                        deleteCandidate = null
                    }
                ) { Text("Only for me") }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = resolvedName,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.clickable {
                                viewModel.openGroupMembersScreen(resolvedName)
                            },
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateUp() }) {
                        Icon(Icons.Rounded.ArrowBackIosNew, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.openPrivateKeyResetDialog() }) {
                        Icon(Icons.Rounded.VpnKey, "Reset encryption key")
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
                        mediaPermissionLauncher.launch(MediaPermissionHandler.getRequiredPermissions())
                    }
                },
                onPickVideo = {
                    if (MediaPermissionHandler.checkAccessLevel(context) != MediaAccessLevel.DENIED) {
                        videoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    } else {
                        pendingMediaType = "video"
                        mediaPermissionLauncher.launch(MediaPermissionHandler.getRequiredPermissions())
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
                                modifier = Modifier.fillMaxSize(),
                                state = groupListState,
                                items = messages,
                                topOverlaySpacerHeight = reservedNowPlayingHeight,
                                messageDeliveryStatuses = messageDeliveryStatuses,
                                canLoadOlder = canLoadOlderHistory,
                                isLoadingOlder = isLoadingOlderHistory,
                                currentUsername = viewModel.getMyUsername(),
                                senderProfileImageUrls = memberProfileImageUrls,
                                onMessageClick = { msg, offset, size ->
                                    selectedMessage = msg
                                    selectedMessageAnchor = offset to size
                                },
                                onAttachmentClick = { msg ->
                                    val url = msg.attachmentUrl
                                    if (!url.isNullOrBlank()) {
                                        when (msg.messageType) {
                                            "image" -> { mediaViewerUrl = url; mediaViewerIsVideo = false }
                                            "video" -> { mediaViewerUrl = url; mediaViewerIsVideo = true }
                                        }
                                    }
                                },
                                onSwipeToReply = { viewModel.setReplyingTo(it) },
                                onPostShareClick = { postId ->
                                    viewModel.openSharedPost(postId)
                                },
                                onLoadOlder = viewModel::loadOlderHistoryIfNeeded
                            )
                        }
                    }
                }

                else -> GroupChatSurface(
                    modifier = Modifier.fillMaxSize(),
                    state = groupListState,
                    items = messages,
                    topOverlaySpacerHeight = reservedNowPlayingHeight,
                    messageDeliveryStatuses = messageDeliveryStatuses,
                    canLoadOlder = canLoadOlderHistory,
                    isLoadingOlder = isLoadingOlderHistory,
                    currentUsername = viewModel.getMyUsername(),
                    senderProfileImageUrls = memberProfileImageUrls,
                    onMessageClick = { msg, offset, size ->
                        selectedMessage = msg
                        selectedMessageAnchor = offset to size
                    },
                    onAttachmentClick = { msg ->
                        val url = msg.attachmentUrl
                        if (!url.isNullOrBlank()) {
                            when (msg.messageType) {
                                "image" -> { mediaViewerUrl = url; mediaViewerIsVideo = false }
                                "video" -> { mediaViewerUrl = url; mediaViewerIsVideo = true }
                            }
                        }
                    },
                    onSwipeToReply = { viewModel.setReplyingTo(it) },
                    onPostShareClick = { postId ->
                        viewModel.openSharedPost(postId)
                    },
                    onLoadOlder = viewModel::loadOlderHistoryIfNeeded
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .onGloballyPositioned { nowPlayingHeightPx = it.size.height }
            ) {
                NowPlayingTopBarRow(onOpenPlaylist = viewModel::openPlaylistBottomSheetIfTrackExists)
            }
        }
    }
}

private fun formatMessageSentAt(timestamp: String?): String {
    if (timestamp.isNullOrBlank()) return "Unknown time"
    return try {
        val sentAt = Instant.parse(timestamp)
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val localTime = sentAt.atZone(zone)
        if (Duration.between(sentAt, now) < Duration.ofDays(1)) {
            localTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        } else {
            localTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
        }
    } catch (_: Exception) {
        timestamp
    }
}

@Composable
private fun GroupMessageMenuRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        TextButton(
            onClick = onClick,
            enabled = enabled
        ) {
            Text(title)
        }
    }
}
