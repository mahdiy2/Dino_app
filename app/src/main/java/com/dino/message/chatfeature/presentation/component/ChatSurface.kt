package com.dino.message.chatfeature.presentation.component

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dino.message.DinoOrderApplication
import com.dino.message.chatfeature.domain.model.MessageDeliveryStatus
import com.dino.message.chatfeature.domain.model.MessageEntity
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import kotlinx.coroutines.flow.distinctUntilChanged

private const val CHAT_SURFACE_TAG = "ChatSurface"

@Composable
fun ChatSurface(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    items: List<MessageEntity>,
    topOverlaySpacerHeight: Dp = 0.dp,
    messageDeliveryStatuses: Map<String, MessageDeliveryStatus> = emptyMap(),
    canLoadOlder: Boolean = false,
    isLoadingOlder: Boolean = false,
    sender: String,
    onMessageClick: ((MessageEntity, IntOffset, IntSize) -> Unit)? = null,
    onAttachmentClick: ((MessageEntity) -> Unit)? = null,
    onPostShareClick: ((Long) -> Unit)? = null,
    onMessageLongClick: ((MessageEntity) -> Unit)? = null,
    onSwipeToReply: ((MessageEntity) -> Unit)? = null,
    onLoadOlder: (() -> Unit)? = null,
) {
    val sanitizedItems = remember(items) {
        val seenKeys = HashSet<String>(items.size)
        val duplicateKeys = mutableSetOf<String>()
        val uniqueItems = ArrayList<MessageEntity>(items.size)
        items.forEach { message ->
            val key = chatItemKey(message)
            if (seenKeys.add(key)) {
                uniqueItems.add(message)
            } else {
                duplicateKeys.add(key)
            }
        }
        uniqueItems to duplicateKeys.toList()
    }
    val renderItems = sanitizedItems.first
    val duplicateKeys = sanitizedItems.second
    val crashReporter = (LocalContext.current.applicationContext as? DinoOrderApplication)?.crashReporter

    LaunchedEffect(duplicateKeys) {
        if (duplicateKeys.isNotEmpty()) {
            // TODO(security): Revisit keeping raw key samples in production logs; redact or remove once duplicate-source bug is fixed.
            val sampleKeys = duplicateKeys.take(3).joinToString(",").take(80)
            Log.w(
                CHAT_SURFACE_TAG,
                "Dropped ${duplicateKeys.size} duplicate chat message key(s): ${duplicateKeys.take(5)}"
            )
            crashReporter?.addBreadcrumb(
                category = "ui_list_keys",
                message = "chat dup=${duplicateKeys.size} total=${items.size} unique=${renderItems.size} sample=$sampleKeys"
            )
        }
    }

    val itemCount = renderItems.size
    val latestMessage = renderItems.firstOrNull()
    val latestMessageKey = latestMessage?.messageId
        ?: latestMessage?.timestamp?.takeIf { it.isNotBlank() }?.let { "ts:$it:${latestMessage.id}" }
        ?: latestMessage?.let { "local:${it.id}" }
    val latestIsMine = latestMessage?.senderOrReceiver != null &&
        latestMessage.senderOrReceiver != sender
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
        reverseLayout = true
    ) {
        itemsIndexed(
            items = renderItems,
            key = { _, item -> chatItemKey(item) }
        ) { _, it ->
            val isMine = it.senderOrReceiver != sender
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DPMedium, vertical = DPSmall),
                horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SwipeToReply(
                    onSwipe = { onSwipeToReply?.invoke(it) },
                    enabled = onSwipeToReply != null
                ) {
                    ChatContainer(
                        text = it.content,
                        sent = isMine,
                        messageType = it.messageType,
                        attachmentUrl = it.attachmentUrl,
                        attachmentMimeType = it.attachmentMimeType,
                        attachmentFileName = it.attachmentFileName,
                        deliveryStatus = if (isMine) it.messageId?.let { messageDeliveryStatuses[it] } else null,
                        onClick = { offset, size -> onMessageClick?.invoke(it, offset, size) },
                        onAttachmentClick = { onAttachmentClick?.invoke(it) },
                        onPostShareClick = onPostShareClick,
                        onLongClick = { onMessageLongClick?.invoke(it) },
                        replySenderName = it.replyToSender,
                        replyPreviewText = it.replyPreview
                    )
                }
            }
        }
        if (isLoadingOlder) {
            item(key = "olderHistoryLoading") {
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

private fun chatItemKey(item: MessageEntity): String {
    return item.messageId?.takeIf { it.isNotBlank() }
        ?: item.timestamp.takeIf { it.isNotBlank() }?.let { "ts:$it:${item.id}" }
        ?: "local:${item.id}"
}
