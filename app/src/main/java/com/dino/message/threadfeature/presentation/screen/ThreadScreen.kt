package com.dino.message.threadfeature.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ListItemDefaults
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dino.message.R
import com.dino.message.corefeature.presentation.util.UserBadgeRole
import com.dino.message.corefeature.presentation.util.parseUserBadgeRole
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.threadfeature.presentation.component.FloatingDockNavigation
import com.dino.message.threadfeature.presentation.component.NowPlayingTopBarRow
import com.dino.message.threadfeature.presentation.component.ThreadPostCard
import com.dino.message.threadfeature.presentation.music.MusicPlaybackManager
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
    val canDownloadPostMedia by viewModel.viewerHasBadge.collectAsState()
    val isBadgeFeaturesDialogDismissed by viewModel.isBadgeFeaturesDialogDismissed.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topBarHeight = 64.dp + statusBarTop
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val postsError by viewModel.error.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val unreadCount by viewModel.unreadNotificationCount.collectAsState()
    val blockFeedback by viewModel.blockFeedback.collectAsState()
    val playlistFeedback by viewModel.playlistFeedback.collectAsState()
    val playbackState by MusicPlaybackManager.playbackState.collectAsState()
    val hasNowPlaying = !playbackState.currentTrackUrl.isNullOrBlank()
    val snackbarHostState = remember { SnackbarHostState() }
    val createOptionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val coroutineScope = rememberCoroutineScope()
    val refreshFromTop: () -> Unit = {
        coroutineScope.launch {
            if (viewModel.posts.isNotEmpty()) {
                listState.animateScrollToItem(0)
            }
            viewModel.refresh()
        }
    }

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
    var nowPlayingHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val nowPlayingHeightDp = with(density) { nowPlayingHeightPx.toDp() }
    val reservedNowPlayingHeight = if (!hasNowPlaying) 0.dp else nowPlayingHeightDp.takeIf { it > 0.dp } ?: 64.dp
    val topBarOffsetY by animateIntOffsetAsState(
        targetValue = if (isDockVisible) IntOffset.Zero
        else IntOffset(0, -topBarHeightPx),
        label = "topBarOffset"
    )
    val nowPlayingTopPadding by animateDpAsState(
        targetValue = if (isDockVisible) topBarHeight else statusBarTop,
        label = "nowPlayingTopPadding"
    )

    LaunchedEffect(hasNowPlaying) {
        if (!hasNowPlaying) nowPlayingHeightPx = 0
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
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

    LaunchedEffect(blockFeedback) {
        blockFeedback?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearBlockFeedback()
        }
    }

    LaunchedEffect(playlistFeedback) {
        playlistFeedback?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearPlaylistFeedback()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.backgroundUploadRefreshEvents.collectLatest {
            refreshFromTop()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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
                        if (reservedNowPlayingHeight > 0.dp) {
                            item(key = "thread_now_playing_spacer") {
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(reservedNowPlayingHeight)
                                )
                            }
                        }

                        if (postsError != null) {
                            item(key = "thread_posts_error_banner") {
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
                                canDownloadMedia = canDownloadPostMedia,
                                selectedPostVote = viewModel.selectedPostVote(post.id),
                                postError = viewModel.postError(post.id),
                                onUpVoteClick = { viewModel.votePost(post.id, 1) },
                                onDownVoteClick = { viewModel.votePost(post.id, -1) },
                                onCommentsClick = { viewModel.openCommentsBottomSheet(post.id) },
                                onQuoteClick = { viewModel.openQuotePostOptions(post) },
                                onShareClick = { viewModel.openShareBottomSheet(post) },
                                onQuotedPostClick = viewModel::openQuotedPost,
                                onReportClick = { viewModel.openReportBottomSheet(post.id) },
                                onBlockUserClick = if (viewModel.shouldShowBlockForAuthor(post.author.username)) {
                                    { viewModel.blockUser(post.author.username) }
                                } else {
                                    null
                                },
                                onUserClick = { viewModel.openUserProfile(post.author.username) },
                                onAddTrackToPlaylistClick = { media ->
                                    viewModel.addTrackToPlaylist(media.id)
                                },
                                onShowTrackPlaylistAddersClick = { media, trackTitle ->
                                    viewModel.openTrackPlaylistAddersDialog(media, trackTitle)
                                }
                            )
                        }

                        if (isLoadingMore) {
                            item(key = "thread_loading_more_indicator") {
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
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    title = {
                        Text(
                            modifier = Modifier
                                .clip(
                                    MaterialTheme.shapes.extraLarge
                                )
                                .clickable(onClick = {
                                    refreshFromTop()
                                }),
                            text = "Dino",
                            style = MaterialTheme.typography.headlineSmall
                        )
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

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = nowPlayingTopPadding)
                    .onGloballyPositioned { nowPlayingHeightPx = it.size.height }
            ) {
                NowPlayingTopBarRow(onOpenPlaylist = viewModel::openPlaylistBottomSheetIfTrackExists)
            }

            AnimatedVisibility(
                visible = isDockVisible,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                enter = slideInVertically(initialOffsetY = { it * 2 }),
                exit = slideOutVertically(targetOffsetY = { it * 2 })
            ) {
                FloatingDockNavigation(
                    selectedIndex = -1,
                    unreadNotificationCount = unreadCount,
                    profileBadge = profile?.badge,
                    profileImageUrl = profile?.profile_image_url,
                    profileImageShape = profile?.profile_image_shape,
                    profileVideoUrl = profile?.profile_video_url,
                    onChatClick = { viewModel.navigateChat() },
                    onPlaylistClick = { viewModel.openPlaylistBottomSheet() },
                    onNotificationsClick = { viewModel.openActivityNotifications() },
                    onProfileClick = { viewModel.openUserProfile() },
                    onFabClick = { viewModel.showCreateOptionsSheet() }
                )
            }

            val shouldShowBadgeFeaturesDialog = canDownloadPostMedia &&
                !isBadgeFeaturesDialogDismissed &&
                !profile?.badge.isNullOrBlank()
            if (shouldShowBadgeFeaturesDialog) {
                BadgeFeaturesActivatedDialog(
                    badge = profile?.badge.orEmpty(),
                    onDismiss = viewModel::dismissBadgeFeaturesDialog
                )
            }
        }
    }

}

@Composable
private fun BadgeFeaturesActivatedDialog(
    badge: String,
    onDismiss: () -> Unit,
) {
    val badgeTypeLabel = when (parseUserBadgeRole(badge)) {
        UserBadgeRole.STAFF -> stringResource(R.string.badge_type_staff)
        UserBadgeRole.MODERATOR -> stringResource(R.string.badge_type_moderator)
        UserBadgeRole.VERIFIED -> stringResource(R.string.badge_type_verified)
        null -> badge.ifBlank { stringResource(R.string.badge_type_unknown) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.42f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopEnd
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.close)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)
                                )
                            ),
                            shape = MaterialTheme.shapes.large
                        )
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_badge_features_activated),
                        contentDescription = null,
                        modifier = Modifier.size(86.dp)
                    )
                }

                Text(
                    text = stringResource(R.string.badge_features_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.badge_features_dialog_message, badgeTypeLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FeaturePill(
                    icon = Icons.Rounded.Download,
                    text = stringResource(R.string.badge_features_dialog_item_post_download)
                )
                FeaturePill(
                    icon = Icons.Rounded.LibraryMusic,
                    text = stringResource(R.string.badge_features_dialog_item_playlist_download)
                )

                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss
                ) {
                    Text(text = stringResource(R.string.got_it))
                }
            }
        }
    }
}

@Composable
private fun FeaturePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
