package com.dino.message.activitynotificationfeature.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dino.message.activitynotificationfeature.presentation.component.ActivityNotificationItem
import com.dino.message.activitynotificationfeature.presentation.component.ActivityNotificationUiItem
import com.dino.message.composeapp.generated.resources.Res
import com.dino.message.composeapp.generated.resources.back
import com.dino.message.composeapp.generated.resources.follow_back
import com.dino.message.composeapp.generated.resources.no_notifications_yet
import com.dino.message.composeapp.generated.resources.notification_write_reply
import com.dino.message.composeapp.generated.resources.notifications
import com.dino.message.composeapp.generated.resources.reply
import com.dino.message.composeapp.generated.resources.send_reply
import com.dino.message.composeapp.generated.resources.cancel
import com.dino.message.corefeature.presentation.icons.AppIcons
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import org.jetbrains.compose.resources.stringResource

data class ActivityNotificationRowState(
    val id: Long,
    val item: ActivityNotificationUiItem,
    val showFollowBackButton: Boolean,
    val isFollowBackLoading: Boolean,
    val showReplyButton: Boolean,
    val isReplyComposerVisible: Boolean,
    val replyDraft: String,
    val isReplySending: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityNotificationContent(
    notifications: List<ActivityNotificationRowState>,
    listState: LazyListState,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    error: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onFollowBackClick: (Long) -> Unit,
    onNotificationClick: (Long) -> Unit,
    onToggleReplyComposer: (Long) -> Unit,
    onReplyDraftChanged: (Long, String) -> Unit,
    onSendReply: (Long) -> Unit,
) {
    val followBackLabel = stringResource(Res.string.follow_back)
    val replyLabel = stringResource(Res.string.reply)
    val cancelLabel = stringResource(Res.string.cancel)
    val replyPlaceholder = stringResource(Res.string.notification_write_reply)
    val sendReplyLabel = stringResource(Res.string.send_reply)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.notifications), style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.ArrowBackIosNew, stringResource(Res.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            if (notifications.isEmpty() && !isRefreshing) {
                Box(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(error ?: stringResource(Res.string.no_notifications_yet))
                }
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(notifications, key = { it.id }) { row ->
                        ActivityNotificationItem(
                            item = row.item,
                            showFollowBackButton = row.showFollowBackButton,
                            followBackLabel = followBackLabel,
                            isFollowBackLoading = row.isFollowBackLoading,
                            onOpenProfile = onOpenProfile,
                            onFollowBackClick = { onFollowBackClick(row.id) },
                            showReplyButton = row.showReplyButton,
                            replyLabel = replyLabel,
                            cancelLabel = cancelLabel,
                            isReplyComposerVisible = row.isReplyComposerVisible,
                            replyDraft = row.replyDraft,
                            replyPlaceholder = replyPlaceholder,
                            sendReplyLabel = sendReplyLabel,
                            isReplySending = row.isReplySending,
                            onNotificationClick = { onNotificationClick(row.id) },
                            onToggleReplyComposer = { onToggleReplyComposer(row.id) },
                            onReplyDraftChanged = { onReplyDraftChanged(row.id, it) },
                            onSendReply = { onSendReply(row.id) },
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(DPMedium),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }
    }
}
