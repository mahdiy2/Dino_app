package com.dino.message.profilefeature.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dino.message.R
import com.dino.message.corefeature.presentation.component.UserBadgeIcon
import com.dino.message.corefeature.presentation.platform.PlatformImageRequestOptions
import com.dino.message.corefeature.presentation.platform.PlatformSubcomposeAsyncImage
import com.dino.message.corefeature.presentation.platform.rememberPlatformImageRequest
import com.dino.message.corefeature.presentation.ui.theme.CardBordersPadding
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.corefeature.presentation.util.resolveProfileImageShape
import com.dino.message.profilefeature.presentation.component.ProfileAvatarMedia
import com.dino.message.profilefeature.presentation.component.ProfileContainer
import com.dino.message.profilefeature.presentation.component.ProfileFollowStatusContainer
import com.dino.message.profilefeature.presentation.viewmodel.ProfileScreenViewModel
import com.dino.message.threadfeature.presentation.component.ThreadPostCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(username: String? = null) {
    val viewModel: ProfileScreenViewModel = koinViewModel()
    val lifecycleOwner = LocalLifecycleOwner.current
    val profile = viewModel.profile.value
    val posts = viewModel.posts
    val isOwnProfile by viewModel.isOwnProfile.collectAsState()
    val isProfileLoading by viewModel.isProfileLoading.collectAsState()
    val isPostsLoading by viewModel.isPostsLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hasMorePosts by viewModel.hasMorePosts.collectAsState()
    val isActionLoading by viewModel.isActionLoading.collectAsState()
    val isFollowing by viewModel.isFollowing.collectAsState()
    val error by viewModel.error.collectAsState()
    val playlistFeedback by viewModel.playlistFeedback.collectAsState()
    val viewerHasBadge by viewModel.viewerHasBadge.collectAsState()
    val profileStoryItem by viewModel.profileStoryItem.collectAsState()
    val isRefreshing = isProfileLoading || isPostsLoading
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var isProfileImageViewerOpen by remember { mutableStateOf(false) }
    var isProfileAvatarMenuExpanded by remember { mutableStateOf(false) }
    val hasProfileImage = !profile?.profile_image_url.isNullOrBlank()
    val hasActiveStory = profileStoryItem?.story_ids?.isNotEmpty() == true
    val hasUnseenStory = profileStoryItem?.has_unseen_story == true

    LaunchedEffect(username) {
        viewModel.initialize(username)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshIfNeededAfterEditProfile()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(playlistFeedback) {
        playlistFeedback?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearPlaylistFeedback()
        }
    }

    LaunchedEffect(listState, posts.size, hasMorePosts) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { lastVisibleIndex ->
                val triggerIndex = (posts.lastIndex - 2).coerceAtLeast(0)
                if (posts.isNotEmpty() && hasMorePosts && lastVisibleIndex >= triggerIndex) {
                    viewModel.loadNextPosts()
                }
            }
    }

    Box(
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
//                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(DPMedium)
            ) {
            item {
                ProfileContainer(
                    modifier = Modifier
                        .fillMaxWidth()

                ) {
                    Column(
                        modifier = Modifier
                            .padding(DPLarge)
                            .windowInsetsPadding(WindowInsets.statusBars)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(DPMedium)
                            ) {
                                Box {
                                    if (hasActiveStory) {
                                        Surface(
                                            modifier = Modifier.size(60.dp),
                                            shape = resolveProfileImageShape(
                                                badge = profile?.badge,
                                                profileImageShape = profile?.profile_image_shape
                                            ),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(
                                                width = 2.5.dp,
                                                color = if (hasUnseenStory) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                                }
                                            )
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(2.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                ProfileAvatarMedia(
                                                    modifier = Modifier.size(56.dp),
                                                    profileImageUrl = profile?.profile_image_url,
                                                    badge = profile?.badge,
                                                    profileImageShape = profile?.profile_image_shape,
                                                    profileVideoUrl = profile?.profile_video_url,
                                                    autoPlayVideo = true,
                                                    onClick = {
                                                        if (hasProfileImage) {
                                                            isProfileAvatarMenuExpanded = true
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    } else {
                                        ProfileAvatarMedia(
                                            modifier = Modifier.size(60.dp),
                                            profileImageUrl = profile?.profile_image_url,
                                            badge = profile?.badge,
                                            profileImageShape = profile?.profile_image_shape,
                                            profileVideoUrl = profile?.profile_video_url,
                                            autoPlayVideo = true,
                                            onClick = {
                                                if (hasProfileImage) {
                                                    isProfileImageViewerOpen = true
                                                }
                                            }
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = isProfileAvatarMenuExpanded && hasProfileImage && hasActiveStory,
                                        onDismissRequest = { isProfileAvatarMenuExpanded = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.view_story)) },
                                            onClick = {
                                                isProfileAvatarMenuExpanded = false
                                                profileStoryItem?.story_ids?.firstOrNull()?.let(viewModel::openProfileStory)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(R.string.profile_image)) },
                                            onClick = {
                                                isProfileAvatarMenuExpanded = false
                                                isProfileImageViewerOpen = true
                                            }
                                        )
                                    }
                                }

                                Column {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = profile?.name?.takeIf { it.isNotBlank() }
                                                ?: profile?.username.orEmpty(),
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        UserBadgeIcon(
                                            badge = profile?.badge,
                                            size = 20.dp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(DPSmall))
                                    Text(
                                        text = "@${profile?.username.orEmpty()}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (isOwnProfile) {

                                Row(
                                ) {
                                    IconButton(
                                        onClick = viewModel::openReferralScreen,
                                        enabled = !isActionLoading
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_referral_program),
                                            contentDescription = stringResource(R.string.referral_program_title)
                                        )
                                    }
                                    IconButton(
                                        onClick = viewModel::openEditProfileScreen,
                                        enabled = !isActionLoading,
                                        modifier = Modifier
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_edit),
                                            contentDescription = null
                                        )
                                    }
                                }

                            } else {
                                FilledTonalButton(
                                    onClick = viewModel::toggleFollow,
                                    enabled = !isActionLoading,
                                    modifier = Modifier
                                ) {
                                    if (isActionLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            if (isFollowing) {
                                                stringResource(R.string.unfollow)
                                            } else {
                                                stringResource(R.string.follow)
                                            }
                                        )
                                    }
                                }
                            }
                        }


                        profile?.bio?.takeIf { it.isNotBlank() }?.let {
                            Spacer(modifier = Modifier.height(DPMedium))
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(DPExtraLarge))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ProfileFollowStatusContainer(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable(onClick = viewModel::openFollowersList),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(vertical = DPSmall)
                                        .width(110.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.followers)
                                    )

                                    Text(
                                        text = "${profile?.followers_count ?: 0}"
                                    )
                                }
                            }

                            ProfileFollowStatusContainer(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable(onClick = viewModel::openFollowingList),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(vertical = DPSmall)
                                        .width(110.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.following)
                                    )
                                    Text(
                                        text = "${profile?.following_count ?: 0}"
                                    )
                                }
                            }

                            ProfileFollowStatusContainer(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.medium),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(vertical = DPSmall)
                                        .width(110.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(text = stringResource(R.string.posts))

                                    Text(text = "${profile?.posts_count ?: 0}")
                                }
                            }
                        }
                    }
                }
            }

            item {

            }

            if (error != null) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(DPMedium),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            modifier = Modifier.padding(DPMedium),
                            text = error!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.posts),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(DPMedium),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if ((isProfileLoading || isPostsLoading) && posts.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(DPMedium)
                            .padding(vertical = DPMedium),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (posts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_empty_posts),
                            contentDescription = null,
                            modifier = Modifier.size(180.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = stringResource(R.string.no_posts_yet),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            textAlign = TextAlign.Center
                        )

                        if (isOwnProfile) {
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = stringResource(R.string.first_post_description),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 48.dp)
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = {
                                    viewModel.showCreateOptionsSheet()
                                },
                                shape = MaterialTheme.shapes.extraLarge,
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {

                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = stringResource(R.string.create_first_post),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                items(
                    items = posts,
                    key = { it.id }
                ) { post ->
                    ThreadPostCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(DPMedium),
                        post = post,
                        canDownloadMedia = viewerHasBadge,
                        selectedPostVote = viewModel.selectedPostVote(post.id),
                        postError = viewModel.postError(post.id),
                        onUpVoteClick = { viewModel.votePost(post.id, 1) },
                        onDownVoteClick = { viewModel.votePost(post.id, -1) },
                        onCommentsClick = { viewModel.openCommentsBottomSheet(post.id) },
                        onQuoteClick = { viewModel.openQuotePostOptions(post) },
                        onShareClick = { viewModel.openShareBottomSheet(post) },
                        onPostLongClick = viewModel::openQuotedPost,
                        onQuotedPostClick = viewModel::openQuotedPost,
                        onReportClick = if (isOwnProfile) null else {
                            { viewModel.openReportBottomSheet(post.id) }
                        },
                        onDeletePostClick = if (isOwnProfile) {
                            { viewModel.openDeletePostBottomSheet(post.id) }
                        } else {
                            null
                        },
                        onEditPostClick = if (isOwnProfile) {
                            { viewModel.openEditPostScreen(post) }
                        } else {
                            null
                        },
                        onAddTrackToPlaylistClick = { media ->
                            viewModel.addTrackToPlaylist(media.id)
                        },
                        onShowTrackPlaylistAddersClick = { media, trackTitle ->
                            viewModel.openTrackPlaylistAddersDialog(media, trackTitle)
                        },
                        showHistoricalWinnerMarkOnProfile = isOwnProfile
                    )
                }

                if (isLoadingMore) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(DPMedium),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
            }
        }

        if (isOwnProfile) {
            FloatingActionButton(
                onClick = viewModel::openProfileSettingsScreen,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.settings)
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = DPMedium)
        )

        if (isProfileImageViewerOpen && !profile?.profile_image_url.isNullOrBlank()) {
            ProfileImageFullScreenDialog(
                imageUrl = profile!!.profile_image_url!!,
                onDismiss = { isProfileImageViewerOpen = false }
            )
        }
    }
}

@Composable
private fun ProfileImageFullScreenDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            PlatformSubcomposeAsyncImage(
                model = rememberPlatformImageRequest(
                    data = imageUrl,
                    options = PlatformImageRequestOptions(crossfade = true)
                ),
                contentDescription = stringResource(R.string.profile_image),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = Color.White
                )
            }
        }
    }
}
