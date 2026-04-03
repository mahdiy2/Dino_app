package com.dino.message.threadfeature.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.profilefeature.presentation.component.ProfileAvatarMedia
import com.dino.message.threadfeature.presentation.component.FloatingDockNavigation
import com.dino.message.threadfeature.presentation.component.ThreadPostCard
import com.dino.message.threadfeature.presentation.viewmodel.ThreadViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Destination<RootGraph>
@Composable
fun ThreadScreen() {
    val viewModel: ThreadViewModel = hiltViewModel()
    val listState = rememberLazyListState()
    val profile = viewModel.profile.value
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topBarHeight = 64.dp + statusBarTop
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val postsError by viewModel.error.collectAsState()
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val unreadCount by viewModel.unreadNotificationCount.collectAsState()
    var showProfileMenu by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    var previousFirstVisibleIndex by remember { mutableIntStateOf(0) }
    val isDockVisible by remember {
        derivedStateOf {
            val currentIndex = listState.firstVisibleItemIndex
            val scrollingUp = currentIndex <= previousFirstVisibleIndex
            previousFirstVisibleIndex = currentIndex
            scrollingUp || currentIndex == 0
        }
    }

    var topBarHeightPx by remember { mutableIntStateOf(0) }
    val topBarOffsetY by animateIntOffsetAsState(
        targetValue = if (isDockVisible) IntOffset.Zero
        else IntOffset(0, -topBarHeightPx),
        label = "topBarOffset"
    )

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshIfNeededAfterCreatePost()
                viewModel.fetchUnreadCount()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(listState, viewModel.posts.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .map { it >= (viewModel.posts.lastIndex - 2).coerceAtLeast(0) }
            .distinctUntilChanged()
            .filter { it && viewModel.posts.isNotEmpty() }
            .collectLatest { viewModel.loadNextPage() }
    }

    Scaffold(
    ) { _ ->
        val animatedTopPadding by animateDpAsState(
            targetValue = if (isDockVisible) topBarHeight else 0.dp,
            label = "topBarPadding"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = animatedTopPadding)
            ) {
                if (viewModel.posts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = postsError ?: "No posts found.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(DPMedium)
                    ) {
                        if (postsError != null) {
                            item {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = DPMedium),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text(
                                        modifier = Modifier.padding(DPMedium),
                                        text = postsError!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        items(
                            items = viewModel.posts,
                            key = { it.id }
                        ) { post ->
                            ThreadPostCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(DPMedium),
                                post = post,
                                selectedPostVote = viewModel.selectedPostVote(post.id),
                                postError = viewModel.postError(post.id),
                                onUpVoteClick = { viewModel.votePost(post.id, 1) },
                                onDownVoteClick = { viewModel.votePost(post.id, -1) },
                                onCommentsClick = { viewModel.openCommentsBottomSheet(post.id) },
                                onUserClick = { viewModel.openUserProfile(post.author.username) }
                            )
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

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { topBarOffsetY }
                    .onGloballyPositioned { topBarHeightPx = it.size.height }
            ) {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    title = {
                        Text(
                            modifier = Modifier
                                .clip(
                                    MaterialTheme.shapes.extraLarge
                                )
                                .clickable(onClick = {
                                    coroutineScope.launch {
                                        if (viewModel.posts.isNotEmpty())
                                            listState.animateScrollToItem(0)
                                        viewModel.refresh()
                                    }
                                }),
                            text = "Threads",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    navigationIcon = {
                        Box {
                            ProfileAvatarMedia(
                               modifier = Modifier
                                   .size(40.dp)
                                   .combinedClickable(
                                       onClick = viewModel::openUserProfile,
                                       onLongClick = { showProfileMenu = true }
                                   ),
                               profileImageUrl = profile?.profile_image_url,
                                profileVideoUrl = profile?.profile_video_url,
                                autoPlayVideo = true
                           )

                            DropdownMenu(
                                expanded = showProfileMenu,
                                onDismissRequest = { showProfileMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Go to Profile") },
                                    onClick = {
                                        showProfileMenu = false
                                        viewModel.openUserProfile()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Notifications") },
                                    onClick = {
                                        showProfileMenu = false
                                        viewModel.openActivityNotifications()
                                    }
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = viewModel::openSearchScreen) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search",
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }

            AnimatedVisibility(
                visible = isDockVisible,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(initialOffsetY = { it * 2 }),
                exit = slideOutVertically(targetOffsetY = { it * 2 })
            ) {
                FloatingDockNavigation(
                    selectedIndex = -1,
                    unreadNotificationCount = unreadCount,
                    onChatClick = { viewModel.navigateChat() },
                    onNotificationsClick = { viewModel.openActivityNotifications() },
                    onFabClick = { viewModel.openCreatePostScreen() }
                )
            }
        }
    }
}
