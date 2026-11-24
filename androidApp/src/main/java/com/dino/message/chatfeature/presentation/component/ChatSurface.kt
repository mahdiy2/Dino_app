package com.dino.message.chatfeature.presentation.component

import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dino.message.DinoOrderApplication
import com.dino.message.chatfeature.domain.model.MessageDeliveryStatus
import com.dino.message.chatfeature.domain.model.MessageEntity
import com.dino.message.chatfeature.domain.util.decodeMessageReactions
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.corefeature.presentation.util.parseServerInstant
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val CHAT_SURFACE_TAG = "ChatSurface"
private val chatDateLabelFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

@OptIn(ExperimentalFoundationApi::class)
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
            Log.w(
                CHAT_SURFACE_TAG,
                "Dropped ${duplicateKeys.size} duplicate chat message key(s)"
            )
            crashReporter?.addBreadcrumb(
                category = "ui_list_keys",
                message = "chat dup=${duplicateKeys.size} total=${items.size} unique=${renderItems.size}"
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
    val decoratedItems = remember(renderItems, sender) {
        buildList(renderItems.size) {
            renderItems.forEachIndexed { index, message ->
                val olderMessage = renderItems.getOrNull(index + 1)
                val dateLabel = dateSeparatorLabelForBoundary(
                    newerTimestamp = message.timestamp,
                    olderTimestamp = olderMessage?.timestamp
                )
                if (dateLabel != null) {
                    add(ChatListEntry.DateHeader("date:${message.timestamp}:${message.id}", dateLabel))
                }

                val isMine = message.senderOrReceiver != sender
                val groupedWithPrevious = olderMessage?.let { previous ->
                    val previousIsMine = previous.senderOrReceiver != sender
                    previousIsMine == isMine &&
                        isSameLocalDay(previous.timestamp, message.timestamp)
                } ?: false
                val bottomPadding = if (groupedWithPrevious) 2.dp else DPMedium
                add(
                    ChatListEntry.MessageRow(
                        key = "msg:${chatItemKey(message)}",
                        message = message,
                        isMine = isMine,
                        topPadding = DPSmall,
                        bottomPadding = bottomPadding
                    )
                )
            }
        }
    }

    LazyColumn(
        modifier = modifier,
        state = state,
        reverseLayout = true,
        contentPadding = PaddingValues(vertical = 6.dp)
    ) {
        items(
            items = decoratedItems,
            key = { entry ->
                when (entry) {
                    is ChatListEntry.DateHeader -> entry.key
                    is ChatListEntry.MessageRow -> entry.key
                }
            }
        ) { entry ->
            when (entry) {
                is ChatListEntry.DateHeader -> {
                    DateSeparatorChip(
                        label = entry.label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .animateItem(
                                placementSpec = tween(durationMillis = 250)
                            )
                    )
                }

                is ChatListEntry.MessageRow -> {
                    val message = entry.message
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = DPMedium,
                                end = DPMedium,
                                top = entry.topPadding,
                                bottom = entry.bottomPadding
                            )
                            .animateItem(
                                placementSpec = tween(durationMillis = 220)
                            ),
                        horizontalArrangement = if (entry.isMine) Arrangement.End else Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SwipeToReply(
                            onSwipe = { onSwipeToReply?.invoke(message) },
                            enabled = onSwipeToReply != null
                        ) {
                            Column(
                                horizontalAlignment = if (entry.isMine) Alignment.End else Alignment.Start
                            ) {
                                ChatContainer(
                                    text = message.content,
                                    sent = entry.isMine,
                                    messageType = message.messageType,
                                    attachmentType = message.attachmentType,
                                    attachmentUrl = message.attachmentUrl,
                                    attachmentMimeType = message.attachmentMimeType,
                                    attachmentFileName = message.attachmentFileName,
                                    deliveryStatus = if (entry.isMine) {
                                        message.messageId?.let { messageDeliveryStatuses[it] }
                                    } else {
                                        null
                                    },
                                    onClick = { offset, size -> onMessageClick?.invoke(message, offset, size) },
                                    onAttachmentClick = { onAttachmentClick?.invoke(message) },
                                    onPostShareClick = onPostShareClick,
                                    onLongClick = { onMessageLongClick?.invoke(message) },
                                    replySenderName = message.replyToSender,
                                    replyPreviewText = message.replyPreview,
                                )
                                val reactions = decodeMessageReactions(message.reactionsJson)
                                if (reactions.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    MessageReactionSummaryRow(
                                        reactions = reactions,
                                        sent = entry.isMine
                                    )
                                }
                            }
                        }
                    }
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

private sealed interface ChatListEntry {
    data class DateHeader(
        val key: String,
        val label: String
    ) : ChatListEntry

    data class MessageRow(
        val key: String,
        val message: MessageEntity,
        val isMine: Boolean,
        val topPadding: Dp,
        val bottomPadding: Dp
    ) : ChatListEntry
}

@Composable
private fun DateSeparatorChip(
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
            tonalElevation = 1.dp
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

private fun dateSeparatorLabelForBoundary(
    newerTimestamp: String?,
    olderTimestamp: String?
): String? {
    val newerDate = timestampToLocalDate(newerTimestamp) ?: return null
    val olderDate = timestampToLocalDate(olderTimestamp)
    if (olderDate == newerDate) return null
    return humanizedDateLabel(newerDate)
}

private fun humanizedDateLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(chatDateLabelFormatter)
    }
}

private fun isSameLocalDay(
    firstTimestamp: String?,
    secondTimestamp: String?
): Boolean {
    val first = timestampToLocalDate(firstTimestamp) ?: return false
    val second = timestampToLocalDate(secondTimestamp) ?: return false
    return first == second
}

private fun timestampToLocalDate(timestamp: String?): LocalDate? {
    val instant = parseServerInstant(timestamp) ?: return null
    return instant.atZone(ZoneId.systemDefault()).toLocalDate()
}
