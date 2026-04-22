package com.dino.message.groupfeature.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dino.message.chatfeature.presentation.component.ChatContainer
import com.dino.message.chatfeature.presentation.component.SwipeToReply
import com.dino.message.chatfeature.domain.model.MessageDeliveryStatus
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.groupfeature.domain.model.GroupMessageEntity
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun GroupChatSurface(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    items: List<GroupMessageEntity>,
    topOverlaySpacerHeight: Dp = 0.dp,
    messageDeliveryStatuses: Map<String, MessageDeliveryStatus> = emptyMap(),
    canLoadOlder: Boolean = false,
    isLoadingOlder: Boolean = false,
    currentUsername: String,
    senderProfileImageUrls: Map<String, String?> = emptyMap(),
    onMessageClick: ((GroupMessageEntity, IntOffset, IntSize) -> Unit)? = null,
    onAttachmentClick: ((GroupMessageEntity) -> Unit)? = null,
    onPostShareClick: ((Long) -> Unit)? = null,
    onMessageLongClick: ((GroupMessageEntity) -> Unit)? = null,
    onSwipeToReply: ((GroupMessageEntity) -> Unit)? = null,
    onLoadOlder: (() -> Unit)? = null,
) {
    val itemCount = items.size
    val latestMessage = items.firstOrNull()
    val latestMessageKey = latestMessage?.messageId
    val latestIsMine = latestMessage?.sender == currentUsername
    LaunchedEffect(latestMessageKey, latestIsMine) {
        if (latestMessageKey == null) return@LaunchedEffect
        val nearLatest = state.firstVisibleItemIndex <= 1
        val shouldSnapToLatest = nearLatest || latestIsMine
        if (shouldSnapToLatest && (state.firstVisibleItemIndex != 0 || state.firstVisibleItemScrollOffset != 0)) {
            state.animateScrollToItem(0)
        }
    }
    LaunchedEffect(state, canLoadOlder, isLoadingOlder, itemCount, onLoadOlder) {
        if (onLoadOlder == null) return@LaunchedEffect
        snapshotFlow {
            if (!canLoadOlder || isLoadingOlder || itemCount == 0) {
                false
            } else {
                val hasUserScrolled = state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 0
                val lastVisibleIndex = state.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } ?: 0
                val threshold = (state.layoutInfo.totalItemsCount - 3).coerceAtLeast(0)
                hasUserScrolled && lastVisibleIndex >= threshold
            }
        }
            .distinctUntilChanged()
            .collect { shouldLoad ->
                if (shouldLoad) {
                    onLoadOlder()
                }
            }
    }
    LazyColumn(
        modifier = modifier,
        state = state,
        reverseLayout = true,
    ) {
        items(items = items, key = { it.messageId }) { message ->
            val isMine = message.sender == currentUsername
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DPMedium, vertical = DPSmall),
                horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isMine) {
                        SenderAvatar(
                            profileImageUrl = senderProfileImageUrls[message.sender],
                            contentDescription = "${message.sender} profile image",
                        )
                        Spacer(modifier = Modifier.size(DPSmall))
                    }
                    Column(
                        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
                    ) {
                        if (!isMine) {
                            Text(
                                text = message.sender,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = DPSmall, bottom = DPSmall)
                            )
                        }
                        SwipeToReply(
                            onSwipe = { onSwipeToReply?.invoke(message) },
                            enabled = onSwipeToReply != null
                        ) {
                            ChatContainer(
                                text = message.content,
                                sent = isMine,
                                messageType = message.messageType,
                                attachmentUrl = message.attachmentUrl,
                                attachmentMimeType = message.attachmentMimeType,
                                attachmentFileName = message.attachmentFileName,
                                deliveryStatus = if (isMine) messageDeliveryStatuses[message.messageId] else null,
                                onClick = { offset, size -> onMessageClick?.invoke(message, offset, size) },
                                onAttachmentClick = { onAttachmentClick?.invoke(message) },
                                onPostShareClick = onPostShareClick,
                                onLongClick = { onMessageLongClick?.invoke(message) },
                                replySenderName = message.replyToSender,
                                replyPreviewText = message.replyPreview
                            )
                        }
                    }
                }
            }
        }
        if (isLoadingOlder) {
            item(key = "olderGroupHistoryLoading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = DPSmall),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        if (topOverlaySpacerHeight > 0.dp) {
            item(key = "nowPlayingTopSpacer") {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(topOverlaySpacerHeight)
                )
            }
        }
    }
}

@Composable
private fun SenderAvatar(
    profileImageUrl: String?,
    contentDescription: String,
) {
    if (profileImageUrl.isNullOrBlank()) {
        Surface(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Icon(
                imageVector = Icons.Rounded.Person,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp),
            )
        }
        return
    }

    val context = LocalContext.current
    val request = remember(context, profileImageUrl) {
        ImageRequest.Builder(context)
            .data(profileImageUrl)
            .crossfade(true)
            .memoryCacheKey(profileImageUrl)
            .diskCacheKey(profileImageUrl)
            .build()
    }

    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape),
    )
}
