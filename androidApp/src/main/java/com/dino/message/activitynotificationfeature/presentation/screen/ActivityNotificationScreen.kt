package com.dino.message.activitynotificationfeature.presentation.screen

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import com.dino.message.corefeature.presentation.viewmodel.rememberStateHolder
import com.dino.message.activitynotificationfeature.presentation.mapper.toUiItem
import com.dino.message.activitynotificationfeature.presentation.viewmodel.ActivityNotificationViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@Composable
fun ActivityNotificationScreen() {
    val viewModel: ActivityNotificationViewModel = rememberStateHolder()
    val context = androidx.compose.ui.platform.LocalContext.current
    val listState = rememberLazyListState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(listState, viewModel.notifications.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .map { it >= (viewModel.notifications.lastIndex - 2).coerceAtLeast(0) }
            .distinctUntilChanged()
            .filter { it && viewModel.notifications.isNotEmpty() }
            .collectLatest { viewModel.loadNextPage() }
    }

    val notificationsById = viewModel.notifications.associateBy { it.id }
    ActivityNotificationContent(
        notifications = viewModel.notifications.map { notification ->
            ActivityNotificationRowState(
                id = notification.id,
                item = notification.toUiItem(context),
                showFollowBackButton = viewModel.shouldShowFollowBack(notification),
                isFollowBackLoading = viewModel.isFollowBackLoading(notification.actor.username),
                showReplyButton = viewModel.canReply(notification),
                isReplyComposerVisible = viewModel.isReplyComposerVisible(notification.id),
                replyDraft = viewModel.replyDraft(notification.id),
                isReplySending = viewModel.isReplySending(notification.id),
            )
        },
        listState = listState,
        isRefreshing = isRefreshing,
        isLoadingMore = isLoadingMore,
        error = error,
        onBack = viewModel::navigateBack,
        onRefresh = viewModel::refresh,
        onOpenProfile = viewModel::openUserProfile,
        onFollowBackClick = { id -> notificationsById[id]?.let { viewModel.followBack(it.actor.username) } },
        onNotificationClick = { id -> notificationsById[id]?.let(viewModel::openNotification) },
        onToggleReplyComposer = viewModel::toggleReplyComposer,
        onReplyDraftChanged = viewModel::onReplyDraftChanged,
        onSendReply = { id -> notificationsById[id]?.let(viewModel::submitReply) },
    )
}
