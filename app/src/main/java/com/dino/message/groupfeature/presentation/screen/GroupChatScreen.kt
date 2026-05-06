package com.dino.message.groupfeature.presentation.screen

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.dino.message.chatfeature.presentation.component.ChatScreenBottomAppBar
import com.dino.message.chatfeature.presentation.component.ChatContainer
import com.dino.message.chatfeature.presentation.component.MessageReactionSummaryRow
import com.dino.message.chatfeature.presentation.component.MediaAccessLevel
import com.dino.message.chatfeature.presentation.component.MediaPermissionHandler
import com.dino.message.chatfeature.presentation.component.MediaViewerDialog
import com.dino.message.chatfeature.domain.model.MessageDeliveryStatus
import com.dino.message.chatfeature.domain.util.decodeMessageReactions
import com.dino.message.R
import com.dino.message.corefeature.presentation.util.backIconModifier
import com.dino.message.groupfeature.domain.model.GroupMessageEntity
import com.dino.message.groupfeature.presentation.component.GroupChatSurface
import com.dino.message.groupfeature.presentation.component.OnlineAvatarBubbleCluster
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
import kotlin.math.min

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
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingDurationMs by viewModel.recordingDurationMs.collectAsState()
    val replyingTo by viewModel.replyingTo.collectAsState()
    val memberProfileImageUrls by viewModel.memberProfileImageUrls.collectAsState()
    val memberProfileImageShapes by viewModel.memberProfileImageShapes.collectAsState()
    val memberBadges by viewModel.memberBadges.collectAsState()
    val onlineGroupUsers by viewModel.onlineGroupUsers.collectAsState()
    val typingGroupUsers by viewModel.typingGroupUsers.collectAsState()
    val context = LocalContext.current
    val isAndroid12Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
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

    DisposableEffect(context) {
        val activity = context.findActivity()
        val window = activity?.window
        val previousSoftInputMode = window?.attributes?.softInputMode
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        onDispose {
            if (previousSoftInputMode != null) {
                window.setSoftInputMode(previousSoftInputMode)
            }
        }
    }

    var mediaViewerUrl by remember { mutableStateOf<String?>(null) }
    var mediaViewerIsVideo by remember { mutableStateOf(false) }
    var pendingMediaType by remember { mutableStateOf<String?>(null) }
    var pendingVoiceAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var selectedMessage by remember { mutableStateOf<GroupMessageEntity?>(null) }
    var selectedMessageAnchor by remember { mutableStateOf<Pair<IntOffset, IntSize>?>(null) }
    var deleteCandidate by remember { mutableStateOf<GroupMessageEntity?>(null) }
    var popupMessage by remember { mutableStateOf<GroupMessageEntity?>(null) }
    var popupMessageAnchor by remember { mutableStateOf<Pair<IntOffset, IntSize>?>(null) }
    var popupSize by remember { mutableStateOf(IntSize.Zero) }
    var popupVisible by remember { mutableStateOf(false) }
    var popupMounted by remember { mutableStateOf(false) }

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
    LaunchedEffect(hasNowPlaying) {
        if (!hasNowPlaying) nowPlayingHeightPx = 0
    }
    LaunchedEffect(selectedMessage, selectedMessageAnchor) {
        if (selectedMessage != null && selectedMessageAnchor != null) {
            popupMessage = selectedMessage
            popupMessageAnchor = selectedMessageAnchor
            popupMounted = true
            popupVisible = false
            withFrameNanos { }
            popupVisible = true
        } else if (popupMounted) {
            popupVisible = false
        }
    }
    LaunchedEffect(popupVisible, popupMounted) {
        if (popupMounted && !popupVisible) {
            kotlinx.coroutines.delay(180)
            popupMessage = null
            popupMessageAnchor = null
            popupMounted = false
        }
    }

    val dismissPopup: () -> Unit = {
        selectedMessage = null
        selectedMessageAnchor = null
        popupVisible = false
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
                context.getString(R.string.group_media_permission_required),
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
        else Toast.makeText(context, context.getString(R.string.microphone_permission_denied), Toast.LENGTH_SHORT).show()
    }

    if (mediaViewerUrl != null) {
        MediaViewerDialog(
            url = mediaViewerUrl!!,
            isVideo = mediaViewerIsVideo,
            onDismiss = { mediaViewerUrl = null }
        )
    }

    if (popupMounted && popupMessage != null && popupMessageAnchor != null) {
        val message = popupMessage!!
        val isMine = message.sender == myUsername
        val (anchorOffset, anchorSize) = popupMessageAnchor!!
        val density = LocalDensity.current
        val configuration = LocalConfiguration.current
        val screenWidthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }
        val screenHeightPx = with(density) { configuration.screenHeightDp.dp.roundToPx() }
        val verticalGapPx = with(density) { 8.dp.roundToPx() }
        val horizontalMarginPx = with(density) { 8.dp.roundToPx() }
        val anchorCenterX = anchorOffset.x + anchorSize.width / 2
        val calculatedX = (anchorCenterX - popupSize.width / 2)
            .coerceIn(
                horizontalMarginPx,
                (screenWidthPx - popupSize.width - horizontalMarginPx).coerceAtLeast(horizontalMarginPx)
            )
        val showAbove = anchorOffset.y >= popupSize.height + verticalGapPx
        val calculatedY = if (showAbove) {
            anchorOffset.y - popupSize.height - verticalGapPx
        } else {
            (anchorOffset.y + anchorSize.height + verticalGapPx)
                .coerceAtMost(screenHeightPx - popupSize.height - verticalGapPx)
        }
        val popupOffset = IntOffset(
            x = calculatedX,
            y = calculatedY.coerceAtLeast(verticalGapPx)
        )
        val transition = updateTransition(
            targetState = popupVisible,
            label = "group_message_popup_transition"
        )
        val popupAlpha by transition.animateFloat(
            transitionSpec = {
                tween(
                    durationMillis = if (targetState) 220 else 140,
                    easing = if (targetState) LinearOutSlowInEasing else FastOutLinearInEasing
                )
            },
            label = "group_popup_alpha"
        ) { visible -> if (visible) 1f else 0f }
        val popupScale by transition.animateFloat(
            transitionSpec = {
                if (targetState) {
                    spring(
                        dampingRatio = 0.72f,
                        stiffness = 520f
                    )
                } else {
                    tween(
                        durationMillis = 140,
                        easing = FastOutLinearInEasing
                    )
                }
            },
            label = "group_popup_scale"
        ) { visible -> if (visible) 1f else 0.86f }
        val popupTranslationY by transition.animateFloat(
            transitionSpec = {
                tween(
                    durationMillis = if (targetState) 240 else 140,
                    easing = if (targetState) LinearOutSlowInEasing else FastOutLinearInEasing
                )
            },
            label = "group_popup_translationY"
        ) { visible ->
            when {
                visible -> 0f
                showAbove -> 18f
                else -> -18f
            }
        }
        val previewAlpha by transition.animateFloat(
            transitionSpec = {
                tween(
                    durationMillis = if (targetState) 180 else 90,
                    delayMillis = if (targetState) 20 else 0,
                    easing = LinearOutSlowInEasing
                )
            },
            label = "group_preview_alpha"
        ) { visible ->
            if (visible) 1f else 0f
        }
        val previewTranslationY by transition.animateFloat(
            transitionSpec = {
                tween(
                    durationMillis = if (targetState) 220 else 100,
                    easing = LinearOutSlowInEasing
                )
            },
            label = "group_preview_translation"
        ) { visible ->
            if (visible) 0f else if (showAbove) 10f else -10f
        }
        val menuAlpha by transition.animateFloat(
            transitionSpec = {
                tween(
                    durationMillis = if (targetState) 200 else 100,
                    delayMillis = if (targetState) 45 else 0,
                    easing = LinearOutSlowInEasing
                )
            },
            label = "group_menu_alpha"
        ) { visible ->
            if (visible) 1f else 0f
        }
        val menuTranslationY by transition.animateFloat(
            transitionSpec = {
                tween(
                    durationMillis = if (targetState) 240 else 100,
                    delayMillis = if (targetState) 30 else 0,
                    easing = LinearOutSlowInEasing
                )
            },
            label = "group_menu_translation"
        ) { visible ->
            if (visible) 0f else if (showAbove) 14f else -14f
        }

        Popup(
            alignment = Alignment.TopStart,
            offset = popupOffset,
            onDismissRequest = dismissPopup,
            properties = PopupProperties(focusable = true)
        ) {
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .widthIn(max = 320.dp)
                    .padding(8.dp)
                    .onGloballyPositioned { popupSize = it.size }
                    .graphicsLayer {
                        alpha = popupAlpha
                        scaleX = popupScale
                        scaleY = popupScale
                        translationY = popupTranslationY
                        transformOrigin = if (showAbove) {
                            TransformOrigin(0.5f, 1f)
                        } else {
                            TransformOrigin(0.5f, 0f)
                        }
                    },
            ) {
                val quickReactions = listOf("👍", "❤️", "😂", "🔥", "😮", "😢")
                Box(
                    modifier = Modifier.graphicsLayer {
                        alpha = previewAlpha
                        translationY = previewTranslationY
                    }
                ) {
                    val topReactions = decodeMessageReactions(message.reactionsJson)
                        .map { it.emoji }
                        .distinct()
                        .take(2)
                    GroupMessageQuickReactionsRow(
                        reactions = (topReactions + quickReactions).distinct(),
                        alignEnd = isMine,
                        onReactionClick = { emoji ->
                            viewModel.reactToMessage(message, emoji)
                            dismissPopup()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (isAndroid12Plus) {
                    Box(
                        modifier = Modifier.graphicsLayer {
                            alpha = previewAlpha
                            translationY = previewTranslationY
                        }
                    ) {
                        GroupMessagePopupPreview(
                            message = message,
                            currentUsername = myUsername,
                            messageDeliveryStatuses = messageDeliveryStatuses
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = menuAlpha
                            translationY = menuTranslationY
                        },
                    contentAlignment = if (isMine) {
                        AbsoluteAlignment.CenterRight
                    } else {
                        AbsoluteAlignment.CenterLeft
                    }
                ) {
                    Surface(
                        modifier = Modifier.wrapContentSize(),
                        shape = RoundedCornerShape(20.dp),
                        tonalElevation = 10.dp,
                        shadowElevation = 10.dp,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 4.dp),
                            horizontalAlignment = if (isMine) {
                                AbsoluteAlignment.Right
                            } else {
                                AbsoluteAlignment.Left
                            }
                        ) {
                            GroupMessageMenuRow(
                                icon = Icons.Rounded.AccessTime,
                                title = formatMessageSentAt(message.timestamp),
                                onClick = {},
                                enabled = false
                            )
                            GroupMessageMenuRow(
                                icon = Icons.Rounded.ContentCopy,
                                title = stringResource(R.string.copy),
                                onClick = {
                                    if (message.content.isNotBlank()) {
                                        val clip = ClipData.newPlainText(context.getString(R.string.message), message.content)
                                        context.getSystemService(android.content.ClipboardManager::class.java)
                                            ?.setPrimaryClip(clip)
                                        Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, context.getString(R.string.nothing_to_copy), Toast.LENGTH_SHORT).show()
                                    }
                                    dismissPopup()
                                }
                            )
                            GroupMessageMenuRow(
                                icon = Icons.Rounded.Delete,
                                title = stringResource(R.string.delete),
                                onClick = {
                                    deleteCandidate = message
                                    dismissPopup()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (deleteCandidate != null) {
        val message = deleteCandidate!!
        val isMine = message.sender == myUsername
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text(stringResource(R.string.delete_message_title)) },
            text = {
                Text(
                    if (isMine) {
                        stringResource(R.string.group_delete_message_body_owner)
                    } else {
                        stringResource(R.string.group_delete_message_body_self_only)
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
                    ) { Text(stringResource(R.string.for_everyone)) }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMessageOnlyForMe(message)
                        deleteCandidate = null
                    }
                ) { Text(stringResource(R.string.only_for_me)) }
            }
        )
    }

    val isMenuOpen = selectedMessage != null
    val blurRadius by animateDpAsState(
        targetValue = if (isMenuOpen) 8.dp else 0.dp,
        animationSpec = tween(
            durationMillis = 260,
            easing = LinearOutSlowInEasing
        ),
        label = "group_scaffold_blur"
    )
    val dimAlpha by animateFloatAsState(
        targetValue = if (isMenuOpen) 0.14f else 0f,
        animationSpec = tween(
            durationMillis = 220,
            easing = LinearOutSlowInEasing
        ),
        label = "group_scaffold_dim"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .blur(blurRadius)
    ) {
        Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.height(146.dp),
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                    ) {
                        if (onlineGroupUsers.isEmpty()) {
                            Text(
                                text = resolvedName,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.clickable {
                                    viewModel.openGroupMembersScreen(resolvedName)
                                },
                            )
                        } else {
                            Text(
                                text = resolvedName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(bottom = 6.dp)
                                    .clickable {
                                        viewModel.openGroupMembersScreen(resolvedName)
                                    },
                            )
                            OnlineAvatarBubbleCluster(
                                onlineUsers = onlineGroupUsers,
                                typingUsers = typingGroupUsers,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .height(54.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.back),
                            modifier = backIconModifier()
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.openPrivateKeyResetDialog() }) {
                        Icon(Icons.Rounded.VpnKey, stringResource(R.string.reset_encryption_key))
                    }
                }
            )
        },
        bottomBar = {
            ChatScreenBottomAppBar(
                onSendClicked = { viewModel.sendTextMessage(it) },
                onInputTextChanged = viewModel::onComposerTextChanged,
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
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
                                        senderProfileImageShapes = memberProfileImageShapes,
                                        senderBadges = memberBadges,
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
                    }

                    else -> CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
                            senderProfileImageShapes = memberProfileImageShapes,
                            senderBadges = memberBadges,
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

    if (isMenuOpen || dimAlpha > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = dimAlpha))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    dismissPopup()
                }
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun formatMessageSentAt(timestamp: String?): String {
    if (timestamp.isNullOrBlank()) return stringResource(R.string.unknown_time)
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
            .padding(horizontal = 10.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(
                onClick = onClick,
                enabled = enabled
            ),
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

@Composable
private fun GroupMessagePopupPreview(
    message: GroupMessageEntity,
    currentUsername: String,
    messageDeliveryStatuses: Map<String, MessageDeliveryStatus>
) {
    val isMine = message.sender == currentUsername

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .wrapContentWidth(
                if (isMine) AbsoluteAlignment.Right else AbsoluteAlignment.Left
            )
    ) {
        Column(
            modifier = Modifier.wrapContentWidth(),
            horizontalAlignment = if (isMine) {
                AbsoluteAlignment.Right
            } else {
                AbsoluteAlignment.Left
            }
        ) {
            ChatContainer(
                text = message.content,
                sent = isMine,
                messageType = message.messageType,
                attachmentType = null,
                attachmentUrl = message.attachmentUrl,
                attachmentMimeType = message.attachmentMimeType,
                attachmentFileName = message.attachmentFileName,
                deliveryStatus = if (isMine) {
                    message.messageId.takeIf { it.isNotBlank() }?.let { messageDeliveryStatuses[it] }
                } else {
                    null
                },
                onClick = { _, _ -> },
                onAttachmentClick = {},
                onPostShareClick = null,
                onLongClick = {},
                replySenderName = message.replyToSender,
                replyPreviewText = message.replyPreview,
            )
            val reactions = decodeMessageReactions(message.reactionsJson)
            if (reactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                MessageReactionSummaryRow(
                    reactions = reactions,
                    sent = isMine
                )
            }
        }
    }
}

@Composable
private fun GroupMessageQuickReactionsRow(
    reactions: List<String>,
    alignEnd: Boolean,
    onReactionClick: (String) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (alignEnd) AbsoluteAlignment.CenterRight else AbsoluteAlignment.CenterLeft
    ) {
        Surface(
            modifier = Modifier.wrapContentWidth(),
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                reactions.take(min(reactions.size, 6)).forEach { emoji ->
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onReactionClick(emoji) }
                    ) {
                        Text(
                            text = emoji,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}
