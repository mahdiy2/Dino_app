package com.dino.message.chatfeature.presentation.component

 import androidx.compose.foundation.layout.Arrangement
 import androidx.compose.foundation.layout.Row
 import androidx.compose.foundation.layout.Spacer
 import androidx.compose.foundation.layout.fillMaxWidth
 import androidx.compose.foundation.layout.height
 import androidx.compose.foundation.layout.padding
 import androidx.compose.foundation.lazy.LazyColumn
 import androidx.compose.foundation.lazy.LazyListState
 import androidx.compose.foundation.lazy.items
 import androidx.compose.foundation.lazy.rememberLazyListState
 import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dino.message.chatfeature.domain.model.MessageDeliveryStatus
import com.dino.message.chatfeature.domain.model.MessageEntity
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall

 @Composable
fun ChatSurface(
     modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    items: List<MessageEntity>,
    topOverlaySpacerHeight: Dp = 0.dp,
    messageDeliveryStatuses: Map<String, MessageDeliveryStatus> = emptyMap(),
    sender: String,
    onMessageClick: ((MessageEntity, IntOffset, IntSize) -> Unit)? = null,
    onAttachmentClick: ((MessageEntity) -> Unit)? = null,
    onPostShareClick: ((Long) -> Unit)? = null,
    onMessageLongClick: ((MessageEntity) -> Unit)? = null,
    onSwipeToReply: ((MessageEntity) -> Unit)? = null
) {
     val itemCount = items.size
     LaunchedEffect(itemCount) {
         if (itemCount > 0 && state.firstVisibleItemIndex <= 1) {
             state.animateScrollToItem(0)
         }
     }
     LazyColumn(
         modifier = modifier,
         state = state,
         reverseLayout = true
     ) {
         items(items = items, key = { it.id }) {
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
