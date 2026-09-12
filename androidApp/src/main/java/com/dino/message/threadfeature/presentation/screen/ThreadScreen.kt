package com.dino.message.threadfeature.presentation.screen

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dino.message.corefeature.presentation.viewmodel.rememberStateHolder
import androidx.lifecycle.Lifecycle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ListItemDefaults
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dino.message.R
import com.dino.message.corefeature.data.upload.AndroidUploadContentProvider
import com.dino.message.corefeature.data.upload.MediaFileResolver
import com.dino.message.corefeature.presentation.util.UserBadgeRole
import com.dino.message.corefeature.presentation.util.parseUserBadgeRole
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.threadfeature.presentation.component.FloatingDockNavigation
import com.dino.message.threadfeature.presentation.component.NowPlayingTopBarRow
import com.dino.message.threadfeature.presentation.component.StoryEditorOverlay
import com.dino.message.threadfeature.presentation.component.StoryRow
import com.dino.message.threadfeature.presentation.component.StoryViewerOverlay
import com.dino.message.threadfeature.presentation.component.ThreadPostCard
import com.dino.message.threadfeature.domain.model.CreatePostMedia
import org.koin.compose.koinInject
import com.dino.message.threadfeature.domain.model.FeedMode
import com.dino.message.threadfeature.presentation.music.MusicPlaybackManager
import com.dino.message.threadfeature.presentation.viewmodel.FollowSuggestion
import com.dino.message.threadfeature.presentation.viewmodel.ThreadViewModel
import com.dino.message.profilefeature.presentation.component.ProfileAvatarMedia
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.dino.message.corefeature.presentation.icons.AppIcons

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ThreadScreen(initialStoryId: Long? = null) {
    val viewModel: ThreadViewModel = rememberStateHolder()
    val listState = rememberLazyListState()
    val profile = viewModel.profile.value
    val canDownloadPostMedia by viewModel.viewerHasBadge.collectAsState()
    val isBadgeFeaturesDialogDismissed by viewModel.isBadgeFeaturesDialogDismissed.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val topBarHeightFallback = 64.dp + statusBarTop
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val postsError by viewModel.error.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val unreadCount by viewModel.unreadNotificationCount.collectAsState()
    val blockFeedback by viewModel.blockFeedback.collectAsState()
    val playlistFeedback by viewModel.playlistFeedback.collectAsState()
    val playbackState by MusicPlaybackManager.playbackState.collectAsState()
    val hasNowPlaying = !playbackState.currentTrackUrl.isNullOrBlank()
    val snackbarHostState = remember { SnackbarHostState() }
    val selectedFeedMode by viewModel.selectedFeedMode.collectAsState()
    val followingSuggestions by viewModel.followingSuggestions.collectAsState()
    val followedSuggestedUsernames by viewModel.followedSuggestedUsernames.collectAsState()
    val isFollowingSuggestionsLoading by viewModel.isFollowingSuggestionsLoading.collectAsState()
    val storyFeed by viewModel.storyFeed.collectAsState()
    val storyFeedError by viewModel.storyFeedError.collectAsState()
    val isStoryFeedLoading by viewModel.isStoryFeedLoading.collectAsState()
    val activeStoryBundle by viewModel.activeStoryBundle.collectAsState()
    val isStoryViewerVisible by viewModel.isStoryViewerVisible.collectAsState()
    val storyActionInFlight by viewModel.storyActionInFlight.collectAsState()
    val storyFeedback by viewModel.storyFeedback.collectAsState()
    val storyViewers by viewModel.storyViewers.collectAsState()
    val storyViewerInsightsVisible by viewModel.storyViewerInsightsVisible.collectAsState()
    val storyViewersLoading by viewModel.storyViewersLoading.collectAsState()
    val storyMentionCandidates by viewModel.storyMentionCandidates.collectAsState()
    val context = LocalContext.current
    val uploadContentProvider: AndroidUploadContentProvider = koinInject()
    var hasOpenedInitialStory by rememberSaveable(initialStoryId) { mutableStateOf(false) }
    var pendingStoryEditorMedia by remember { mutableStateOf<CreatePostMedia?>(null) }

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
    val topBarHeight = if (topBarHeightPx > 0) {
        with(density) { topBarHeightPx.toDp() }
    } else {
        topBarHeightFallback
    }
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

    LaunchedEffect(initialStoryId, hasOpenedInitialStory) {
        val storyId = initialStoryId ?: return@LaunchedEffect
        if (!hasOpenedInitialStory && storyId > 0L) {
            hasOpenedInitialStory = true
            viewModel.openStoryById(storyId)
        }
    }

    BackHandler(enabled = isStoryViewerVisible && !storyViewerInsightsVisible) {
        viewModel.closeStoryViewer()
    }

    LaunchedEffect(pendingStoryEditorMedia != null) {
        if (pendingStoryEditorMedia != null) {
            viewModel.requestStoryMentionCandidates(query = "")
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.fetchUnreadCount()
                viewModel.refreshAfterPostIfNeeded()
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

    LaunchedEffect(storyFeedback) {
        storyFeedback?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearStoryFeedback()
        }
    }

    val storyPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val contentResolver = context.contentResolver
            val guessedFileName = MediaFileResolver.queryDisplayName(contentResolver, uri)
                ?: uri.lastPathSegment
                    ?.substringAfterLast('/')
                    ?.ifBlank { "story_media" }
                ?: "story_media"
            val normalizedMimeType = MediaFileResolver.resolveMimeType(
                contentResolver = contentResolver,
                uri = uri,
                displayNameHint = guessedFileName,
            )
            pendingStoryEditorMedia = CreatePostMedia(
                file = uploadContentProvider.readableMediaFile(
                    uri = uri,
                    displayName = guessedFileName,
                    mimeType = normalizedMimeType,
                )
            )
        }
    }

    val launchNewStoryPicker = {
        storyPickerLauncher.launch(
            PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageAndVideo
            )
        )
    }
    val ownStory = storyFeed.firstOrNull { it.username == viewModel.viewerUsername }

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

                    item(key = "thread_story_row") {
                        StoryRow(
                            items = storyFeed.filter { it.username != viewModel.viewerUsername },
                            viewerProfile = profile,
                            hasActiveYourStory = ownStory?.story_ids?.isNotEmpty() == true,
                            onViewYourStory = {
                                ownStory?.let(viewModel::openStoryForUser)
                            },
                            onCreateNewStory = launchNewStoryPicker,
                            onStoryClick = viewModel::openStoryForUser,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = DPMedium)
                        )
                    }

                    if (storyFeedError != null) {
                        item(key = "thread_story_error_banner") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = DPMedium),
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(
                                    modifier = Modifier.padding(DPMedium),
                                    text = storyFeedError!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

//                    if (isStoryFeedLoading && storyFeed.isEmpty()) {
//                        item(key = "thread_story_loading") {
//                            Box(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .padding(vertical = DPMedium),
//                                contentAlignment = Alignment.Center
//                            ) {
//                                CircularProgressIndicator()
//                            }
//                        }
//                    }

                    item(key = "thread_feed_mode_switcher") {
                        ExpressiveFeedModeTabs(
                            selectedMode = selectedFeedMode,
                            onModeSelected = viewModel::onFeedModeSelected,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = DPMedium)
                        )
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

                    if (selectedFeedMode == FeedMode.FOLLOWERS && viewModel.posts.isEmpty() && !isRefreshing) {
                        item(key = "thread_following_empty_suggestions") {
                            FollowingSuggestionsSection(
                                suggestions = followingSuggestions,
                                followedUsernames = followedSuggestedUsernames,
                                isLoading = isFollowingSuggestionsLoading,
                                onFollowClick = viewModel::followSuggestedUser,
                                onConfirm = viewModel::confirmFollowSuggestions,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = DPMedium)
                            )
                        }
                    } else if (viewModel.posts.isEmpty()) {
                        item(key = "thread_posts_empty_state") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = DPMedium, vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = postsError ?: stringResource(R.string.thread_posts_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
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
                                onPostLongClick = viewModel::openQuotedPost,
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
                                imageVector = AppIcons.Search,
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

            if (isStoryViewerVisible && activeStoryBundle != null) {
                StoryViewerOverlay(
                    bundle = activeStoryBundle!!,
                    viewerUsername = viewModel.viewerUsername,
                    viewers = storyViewers,
                    isActionInFlight = storyActionInFlight,
                    showInsights = storyViewerInsightsVisible,
                    isInsightsLoading = storyViewersLoading,
                    onDismiss = viewModel::closeStoryViewer,
                    onStoryShown = viewModel::recordStoryView,
                    onLikeStory = viewModel::setStoryLike,
                    onReplyStory = viewModel::replyToActiveStory,
                    onOpenChatWithOwner = viewModel::openChatWithUser,
                    onOpenNextStoryBundle = viewModel::openNextStoryBundleOrClose,
                    onOpenPreviousStoryBundle = viewModel::openPreviousStoryBundleOrClose,
                    onDeleteStory = viewModel::deleteStory,
                    onLoadInsights = viewModel::fetchStoryViewers,
                    onDismissInsights = viewModel::dismissStoryInsights,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            pendingStoryEditorMedia?.let { draftMedia ->
                StoryEditorOverlay(
                    media = draftMedia,
                    mentionSuggestions = storyMentionCandidates,
                    onMentionQueryChanged = viewModel::requestStoryMentionCandidates,
                    onDismiss = { pendingStoryEditorMedia = null },
                    onPost = { editedMedia, mentions ->
                        pendingStoryEditorMedia = null
                        viewModel.uploadStory(media = editedMedia, mentions = mentions)
                    },
                    onEditorNotice = { message ->
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(message)
                        }
                    },
                    modifier = Modifier.align(Alignment.Center)
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

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
private fun ExpressiveFeedModeTabs(
    selectedMode: FeedMode,
    onModeSelected: (FeedMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabItems = listOf(
        FeedMode.FOLLOWERS to stringResource(R.string.feed_mode_followers),
        FeedMode.MIXED to stringResource(R.string.feed_mode_mixed),
        FeedMode.MUSIC to stringResource(R.string.feed_mode_music),
    )

    val selectedIndex = tabItems.indexOfFirst { it.first == selectedMode }.coerceAtLeast(0)
    val horizontalInset = 6.dp
    val tabSpacing = 6.dp

    val interactionSource = remember { MutableInteractionSource() }


    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalInset, vertical = 6.dp)
        ) {
            val tabCount = tabItems.size
            val tabWidth = (maxWidth - (tabSpacing * (tabCount - 1))) / tabCount
            val indicatorOffsetTarget =
                (tabWidth + tabSpacing) * selectedIndex
            val indicatorOffset by animateDpAsState(
                targetValue = indicatorOffsetTarget,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = 0.78f,
                    stiffness = 520f
                ),
                label = "feedTabsIndicatorOffset"
            )
            val indicatorColor by animateColorAsState(
                targetValue = MaterialTheme.colorScheme.primaryContainer,
                label = "feedTabsIndicatorColor"
            )

            Surface(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(tabWidth)
                    .requiredHeightIn(min = 44.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = indicatorColor,
                tonalElevation = 2.dp,
                shadowElevation = 0.dp
            ) {}

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(tabSpacing)
            ) {
                tabItems.forEach { (mode, label) ->
                    val selected = mode == selectedMode
                    val animatedTextColor by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        label = "feedTabTextColor"
                    )

                    Surface(
                        modifier = Modifier
                            .width(tabWidth)
                            .requiredHeightIn(min = 44.dp)
                            .clip(MaterialTheme.shapes.extraLarge)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                onModeSelected(mode)
                            },
                        color = Color.Transparent,
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 12.dp),
                                text = label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = if (selected) {
                                    MaterialTheme.typography.labelLarge
                                } else {
                                    MaterialTheme.typography.labelMedium
                                },
                                color = animatedTextColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowingSuggestionsSection(
    suggestions: List<FollowSuggestion>,
    followedUsernames: Set<String>,
    isLoading: Boolean,
    onFollowClick: (String) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.following_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.following_empty_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (suggestions.isEmpty()) {
                Text(
                    text = stringResource(R.string.following_empty_no_suggestions),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                suggestions.forEach { suggestion ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProfileAvatarMedia(
                            modifier = Modifier.size(48.dp),
                            profileImageUrl = suggestion.profileImageUrl,
                            profileVideoUrl = null,
                            badge = suggestion.badge,
                            profileImageShape = suggestion.profileImageShape
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = suggestion.username,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!suggestion.name.isNullOrBlank()) {
                                Text(
                                    text = suggestion.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        val followed = followedUsernames.contains(suggestion.username)
                        if (followed) {
                            FilledTonalButton(
                                enabled = false,
                                onClick = {}
                            ) {
                                Text(text = stringResource(R.string.following_suggestion_following))
                            }
                        } else {
                            OutlinedButton(onClick = { onFollowClick(suggestion.username) }) {
                                Text(text = stringResource(R.string.following_suggestion_follow))
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onConfirm,
                enabled = followedUsernames.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors()
            ) {
                Text(text = stringResource(R.string.following_suggestion_confirm))
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
        UserBadgeRole.POST_OF_THE_DAY -> stringResource(R.string.badge_type_post_of_day)
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
                            imageVector = AppIcons.Close,
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
                    icon = AppIcons.Download,
                    text = stringResource(R.string.badge_features_dialog_item_post_download)
                )
                FeaturePill(
                    icon = AppIcons.LibraryMusic,
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
