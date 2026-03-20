package com.dino.message.chatfeature.presentation.screen

import android.Manifest
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val isPublicKeyLoading by viewModel.isPublicKeyLoading.collectAsState()
    val publicKeyError by viewModel.publicKeyError.collectAsState()
    val isPublicKeyReady by viewModel.isPublicKeyReady.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingDurationMs by viewModel.recordingDurationMs.collectAsState()
    val context = LocalContext.current
    val resolvedName = displayName ?: chatId
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingVoiceAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var mediaViewerUrl by remember { mutableStateOf<String?>(null) }
    var mediaViewerIsVideo by remember { mutableStateOf(false) }
    var pendingMediaType by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uploadError) {
        uploadError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissUploadError()
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
                            profileVideoUrl = profileVideoUrl
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
                enabled = isPublicKeyReady
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
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
                                    items = viewModel.messages,
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
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
