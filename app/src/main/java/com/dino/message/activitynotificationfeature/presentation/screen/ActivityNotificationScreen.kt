package com.dino.message.activitynotificationfeature.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dino.message.R
import com.dino.message.activitynotificationfeature.presentation.component.ActivityNotificationItem
import com.dino.message.activitynotificationfeature.presentation.viewmodel.ActivityNotificationViewModel
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.util.backIconModifier
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ActivityNotificationScreen() {
    val viewModel: ActivityNotificationViewModel = hiltViewModel()
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.notifications),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.back),
                            modifier = backIconModifier()
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (viewModel.notifications.isEmpty() && !isRefreshing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = error ?: stringResource(R.string.no_notifications_yet),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = viewModel.notifications,
                        key = { it.id }
                    ) { notification ->
                        ActivityNotificationItem(
                            notification = notification,
                            showFollowBackButton = viewModel.shouldShowFollowBack(notification),
                            isFollowBackLoading = viewModel.isFollowBackLoading(notification.actor.username),
                            onOpenProfile = viewModel::openUserProfile,
                            onFollowBackClick = { viewModel.followBack(notification.actor.username) },
                            showReplyButton = viewModel.canReply(notification),
                            isReplyComposerVisible = viewModel.isReplyComposerVisible(notification.id),
                            replyDraft = viewModel.replyDraft(notification.id),
                            isReplySending = viewModel.isReplySending(notification.id),
                            onToggleReplyComposer = { viewModel.toggleReplyComposer(notification.id) },
                            onReplyDraftChanged = { viewModel.onReplyDraftChanged(notification.id, it) },
                            onSendReply = { viewModel.submitReply(notification) }
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }

                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(DPMedium),
                                contentAlignment = Alignment.Center
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
