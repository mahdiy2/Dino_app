package com.dino.message.chatfeature.presentation.screen

import android.Manifest
import android.app.Activity
import android.os.Build
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material3.Badge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import com.dino.message.R
import com.dino.message.corefeature.presentation.component.UserBadgeIcon
import com.dino.message.corefeature.presentation.platform.LocalPlatformUiEffect
import com.dino.message.corefeature.presentation.util.backIconModifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.koin.compose.viewmodel.koinViewModel
import com.dino.message.chatfeature.domain.model.MessageDeliveryStatus
import com.dino.message.chatfeature.domain.util.decodeMessageReactions
import com.dino.message.chatfeature.presentation.component.ChatScreenBottomAppBar
import com.dino.message.chatfeature.presentation.component.ChatSurface
import com.dino.message.chatfeature.presentation.component.MessageReactionSummaryRow
import com.dino.message.chatfeature.presentation.component.MediaAccessLevel
import com.dino.message.chatfeature.presentation.component.MediaPermissionHandler
import com.dino.message.chatfeature.presentation.component.MediaViewerDialog
import com.dino.message.chatfeature.domain.model.MessageEntity
import com.dino.message.chatfeature.presentation.component.ChatContainer
import com.dino.message.chatfeature.presentation.viewmodel.ChatViewModel
import com.dino.message.corefeature.data.upload.toPlatformMediaReference
import com.dino.message.profilefeature.presentation.component.ProfileAvatarMedia
import com.dino.message.threadfeature.presentation.component.NowPlayingTopBarRow
import com.dino.message.threadfeature.presentation.music.MusicPlaybackManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.min
import com.dino.message.corefeature.presentation.icons.AppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    displayName: String? = null,
    badge: String? = null,
    profileImageUrl: String? = null,
    profileImageShape: String? = null,
    profileVideoUrl: String? = null
) {
    val viewModel: ChatViewModel = koinViewModel()
    val isPeerOnline by viewModel.isPeerOnline.collectAsState()
    val isUploading by viewModel.isUploading.collectAsState()
    val isLoadingOlderHistory by viewModel.isLoadingOlderHistory.collectAsState()
    val canLoadOlderHistory by viewModel.canLoadOlderHistory.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val messageDeliveryStatuses by viewModel.messageDeliveryStatuses.collectAsState()
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
    val showBlockedDialog by viewModel.showBlockedDialog.collectAsState()
    val blockedDialogMessage by viewModel.blockedDialogMessage.collectAsState()
    val debugListState = rememberLazyListState()
    val chatListState = rememberLazyListState()
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingDurationMs by viewModel.recordingDurationMs.collectAsState()
    val replyingTo by viewModel.replyingTo.collectAsState()
    val context = LocalContext.current
    val platformUiEffect = LocalPlatformUiEffect.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val isAndroid12Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val resolvedName = displayName ?: chatId
    val snackbarHostState = remember { SnackbarHostState() }
    val playbackState by MusicPlaybackManager.playbackState.collectAsState()
    val hasNowPlaying = !playbackState.currentTrackUrl.isNullOrBlank()
    var nowPlayingHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val nowPlayingHeightDp = with(density) { nowPlayingHeightPx.toDp() }
    val reservedNowPlayingHeight =
        if (!hasNowPlaying) 0.dp else nowPlayingHeightDp.takeIf { it > 0.dp } ?: 64.dp

    var pendingVoiceAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var mediaViewerUrl by remember { mutableStateOf<String?>(null) }
    var mediaViewerIsVideo by remember { mutableStateOf(false) }
    var pendingMediaType by remember { mutableStateOf<String?>(null) }
    var selectedMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var selectedMessageAnchor by remember { mutableStateOf<Pair<IntOffset, IntSize>?>(null) }
    var deleteCandidate by remember { mutableStateOf<MessageEntity?>(null) }
    var popupMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var popupMessageAnchor by remember { mutableStateOf<Pair<IntOffset, IntSize>?>(null) }

    var showMenu by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner, activity) {
        if (activity == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> WindowKeyboardController.enableResize(activity)
                    Lifecycle.Event.ON_PAUSE -> WindowKeyboardController.restore(activity)
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.RESUMED) {
                WindowKeyboardController.enableResize(activity)
            }
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                WindowKeyboardController.restore(activity)
            }
        }
    }

    LaunchedEffect(uploadError) {
        uploadError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissUploadError()
        }
    }

    LaunchedEffect(decryptionError) {
        decryptionError?.let {
            val result = snackbarHostState.showSnackbar(
                message = it,
                actionLabel = context.getString(R.string.change_key),
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.openPrivateKeyResetDialog()
            }
            viewModel.dismissDecryptionError()
        }
    }

    LaunchedEffect(socketError) {
        socketError?.let {
            val result = snackbarHostState.showSnackbar(
                message = it,
                actionLabel = context.getString(R.string.retry),
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
    LaunchedEffect(hasNowPlaying) {
        if (!hasNowPlaying) nowPlayingHeightPx = 0
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.sendAttachment(it.toPlatformMediaReference()) }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.sendAttachment(it.toPlatformMediaReference()) }
    }

    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val accessLevel = MediaPermissionHandler.resolveAccessLevel(permissions)
        if (accessLevel == MediaAccessLevel.DENIED) {
            platformUiEffect.showToast(context.getString(R.string.chat_media_permission_required))
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
            platformUiEffect.showToast(context.getString(R.string.microphone_permission_required))
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

    if (showBlockedDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.acknowledgeBlockedDialogAndExit() },
            title = { Text(stringResource(R.string.chat_unavailable_title)) },
            text = {
                Text(
                    blockedDialogMessage
                        ?: stringResource(R.string.chat_blocked_message)
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.acknowledgeBlockedDialogAndExit() }) {
                    Text(stringResource(R.string.ok_upper))
                }
            }
        )
    }
    var popupSize by remember { mutableStateOf(IntSize.Zero) }

    var popupVisible by remember { mutableStateOf(false) }
    var popupMounted by remember { mutableStateOf(false) }

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

    val dismissPopup: () -> Unit = {
        selectedMessage = null
        selectedMessageAnchor = null
        popupVisible = false
    }

    LaunchedEffect(popupVisible, popupMounted) {
        if (popupMounted && !popupVisible) {
            kotlinx.coroutines.delay(180)
            popupMessage = null
            popupMessageAnchor = null
            popupMounted = false
        }
    }


    if (popupMounted && popupMessage != null && popupMessageAnchor != null) {
        val message = popupMessage!!
        val isMine = message.senderOrReceiver != chatId
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
                (screenWidthPx - popupSize.width - horizontalMarginPx)
                    .coerceAtLeast(horizontalMarginPx)
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
            label = "message_popup_transition"
        )

        val popupAlpha by transition.animateFloat(
            transitionSpec = {
                tween(
                    durationMillis = if (targetState) 220 else 140,
                    easing = if (targetState) LinearOutSlowInEasing else FastOutLinearInEasing
                )
            },
            label = "popup_alpha"
        ) { visible ->
            if (visible) 1f else 0f
        }

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
            label = "popup_scale"
        ) { visible ->
            if (visible) 1f else 0.86f
        }

        val popupTranslationY by transition.animateFloat(
            transitionSpec = {
                tween(
                    durationMillis = if (targetState) 240 else 140,
                    easing = if (targetState) LinearOutSlowInEasing else FastOutLinearInEasing
                )
            },
            label = "popup_translationY"
        ) { visible ->
            when {
                visible -> 0f
                showAbove -> 18f   // وقتی بالا باز میشه، در حالت بسته کمی پایین‌تر باشد
                else -> -18f       // وقتی پایین باز میشه، در حالت بسته کمی بالاتر باشد
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
            label = "preview_alpha"
        ) { visible ->
            if (visible) 1f else 0f
        }

        val previewTranslationY by transition.animateFloat(
            transitionSpec = {
                tween(
                    durationMillis = if (targetState) 220 else 100,
                    delayMillis = if (targetState) 0 else 0,
                    easing = LinearOutSlowInEasing
                )
            },
            label = "preview_translation"
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
            label = "menu_alpha"
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
            label = "menu_translation"
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
                    }

                    .onGloballyPositioned { popupSize = it.size },
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
                    MessageQuickReactionsRow(
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
                        MessagePopupPreview(
                            message = message,
                            sender = chatId,
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
                            MessageMenuRow(
                                icon = AppIcons.AccessTime,
                                title = formatMessageSentAt(message.timestamp),
                                onClick = {},
                                enabled = false
                            )

                            MessageMenuRow(
                                icon = AppIcons.ContentCopy,
                                title = stringResource(R.string.copy),
                                onClick = {
                                    if (message.content.isNotBlank()) {
                                        platformUiEffect.copyPlainText(
                                            label = context.getString(R.string.message),
                                            text = message.content,
                                        )
                                        platformUiEffect.showToast(context.getString(R.string.copied_to_clipboard))
                                    } else {
                                        platformUiEffect.showToast(context.getString(R.string.nothing_to_copy))
                                    }
                                    dismissPopup()
                                }
                            )

                            MessageMenuRow(
                                icon = AppIcons.Delete,
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
//            }

        }
    }


    if (deleteCandidate != null) {
        val message = deleteCandidate!!
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text(stringResource(R.string.delete_message_title)) },
            text = { Text(stringResource(R.string.delete_message_body_both_sides)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMessageForEveryone(message)
                        deleteCandidate = null
                    }
                ) { Text(stringResource(R.string.for_both_sides)) }
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
        label = "scaffold_blur"
    )

    val dimAlpha by animateFloatAsState(
        targetValue = if (isMenuOpen) 0.14f else 0f,
        animationSpec = tween(
            durationMillis = 220,
            easing = LinearOutSlowInEasing
        ),
        label = "scaffold_dim"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .blur(blurRadius)
    ) {
        Scaffold(
            contentWindowInsets = WindowInsets(0),
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            ProfileAvatarMedia(
                                modifier = Modifier.size(36.dp),
                                profileImageUrl = profileImageUrl,
                                badge = badge,
                                profileImageShape = profileImageShape,
                                profileVideoUrl = profileVideoUrl,
                                autoPlayVideo = true,
                                onClick = viewModel::openPeerProfile
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = resolvedName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    UserBadgeIcon(badge = badge)
                                }
                                Text(
                                    text = if (isPeerOnline) {
                                        stringResource(R.string.online)
                                    } else {
                                        stringResource(R.string.offline)
                                    },
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
                        Box {
                            IconButton(
                                onClick = { showMenu = true }
                            ) {
                                Icon(
                                    imageVector = AppIcons.MoreVert,
                                    contentDescription = null
                                )
                            }

                            DropdownMenu(
                                shape = MaterialTheme.shapes.extraLarge,
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(stringResource(R.string.reset_encryption_key))
                                    },
                                    onClick = {
                                        showMenu = false
                                        viewModel.openPrivateKeyResetDialog()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = AppIcons.VpnKey,
                                            contentDescription = null
                                        )
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(stringResource(R.string.clear))
                                    },
                                    onClick = {
                                        showMenu = false
                                        viewModel.clearMessages()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = AppIcons.Delete,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.navigateUp() }) {
                            Icon(
                                imageVector = AppIcons.ArrowBackIosNew,
                                contentDescription = stringResource(R.string.back),
                                modifier = backIconModifier()
                            )
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
                        pendingVoiceAction = { viewModel.startVoiceRecording() }
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
                                text = publicKeyError
                                    ?: stringResource(R.string.failed_to_load_encryption_key),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.retryLoadPublicKey() }) {
                                Text(stringResource(R.string.retry))
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
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        ChatSurface(
                                            modifier = Modifier.fillMaxSize(),
                                            state = chatListState,
                                            items = messages,
                                            topOverlaySpacerHeight = reservedNowPlayingHeight,
                                            messageDeliveryStatuses = messageDeliveryStatuses,
                                            canLoadOlder = canLoadOlderHistory,
                                            isLoadingOlder = isLoadingOlderHistory,
                                            sender = chatId,
                                            onMessageClick = { message, offset, size ->
                                                selectedMessage = message
                                                selectedMessageAnchor = offset to size
                                            },
                                            onAttachmentClick = { message ->
                                                val openedStory =
                                                    viewModel.openStoryFromAttachment(message)
                                                if (openedStory) {
                                                    return@ChatSurface
                                                }
                                                val url = message.attachmentUrl
                                                if (!url.isNullOrBlank()) {
                                                    val normalizedAttachmentType =
                                                        message.attachmentType
                                                            ?.trim()
                                                            ?.lowercase()
                                                    val isStoryAttachment =
                                                        normalizedAttachmentType == "story_reply" ||
                                                                normalizedAttachmentType == "story_mention"
                                                    val storyIsVideo = message.attachmentMimeType
                                                        ?.contains("video", ignoreCase = true) == true
                                                    when {
                                                        isStoryAttachment && storyIsVideo -> {
                                                            mediaViewerUrl = url
                                                            mediaViewerIsVideo = true
                                                        }

                                                        isStoryAttachment || message.messageType == "image" -> {
                                                            mediaViewerUrl = url
                                                            mediaViewerIsVideo = false
                                                        }

                                                        message.messageType == "video" -> {
                                                            mediaViewerUrl = url
                                                            mediaViewerIsVideo = true
                                                        }

                                                        else -> Unit
                                                    }
                                                }
                                            },
                                            onSwipeToReply = { message ->
                                                viewModel.setReplyingTo(message)
                                            },
                                            onPostShareClick = { postId ->
                                                viewModel.openSharedPost(postId)
                                            },
                                            onLoadOlder = viewModel::loadOlderHistoryIfNeeded
                                        )
                                    }
                                }
                            }
                        }
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
                                text = stringResource(R.string.socket_debug_log),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Row {
                                IconButton(onClick = {
                                    val allLogs = socketDebugEvents.joinToString("\n")
                                    platformUiEffect.copyPlainText(
                                        label = context.getString(R.string.socket_debug_log),
                                        text = allLogs,
                                    )
                                    platformUiEffect.showToast(context.getString(R.string.copied_to_clipboard))
                                }) {
                                    Icon(
                                        imageVector = AppIcons.ContentCopy,
                                        contentDescription = stringResource(R.string.copy_all)
                                    )
                                }
                                IconButton(onClick = { showDebugLog = false }) {
                                    Icon(
                                        imageVector = AppIcons.Close,
                                        contentDescription = stringResource(R.string.close)
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
                                    text = stringResource(R.string.no_events_yet),
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
                                            modifier = Modifier.padding(
                                                horizontal = 8.dp,
                                                vertical = 4.dp
                                            ),
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
private fun MessageMenuRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
//            .fillMaxWidth()
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
private fun MessagePopupPreview(
    message: MessageEntity,
    sender: String,
    messageDeliveryStatuses: Map<String, MessageDeliveryStatus>
) {
    val isMine = message.senderOrReceiver != sender

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .wrapContentWidth(
                if (isMine) AbsoluteAlignment.Right else AbsoluteAlignment.Left
            ),
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
                attachmentType = message.attachmentType,
                attachmentUrl = message.attachmentUrl,
                attachmentMimeType = message.attachmentMimeType,
                attachmentFileName = message.attachmentFileName,
                deliveryStatus = if (isMine) {
                    message.messageId?.let { messageDeliveryStatuses[it] }
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
private fun MessageQuickReactionsRow(
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
