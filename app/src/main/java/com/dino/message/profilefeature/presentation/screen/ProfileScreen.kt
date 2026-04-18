package com.dino.message.profilefeature.presentation.screen

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.SubcomposeAsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.dino.message.R
import com.dino.message.corefeature.presentation.ui.theme.CardBordersPadding
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.profilefeature.presentation.component.ProfileAvatarMedia
import com.dino.message.profilefeature.presentation.component.ProfileContainer
import com.dino.message.profilefeature.presentation.component.ProfileFollowStatusContainer
import com.dino.message.profilefeature.presentation.viewmodel.ProfileScreenViewModel
import com.dino.message.threadfeature.presentation.component.PostContainer
import com.dino.message.threadfeature.presentation.component.ThreadPostCard
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ProfileScreen(username: String? = null) {
    val viewModel: ProfileScreenViewModel = hiltViewModel()
    val lifecycleOwner = LocalLifecycleOwner.current
    val profile = viewModel.profile.value
    val posts = viewModel.posts
    val isOwnProfile by viewModel.isOwnProfile.collectAsState()
    val isProfileLoading by viewModel.isProfileLoading.collectAsState()
    val isPostsLoading by viewModel.isPostsLoading.collectAsState()
    val isActionLoading by viewModel.isActionLoading.collectAsState()
    val isFollowing by viewModel.isFollowing.collectAsState()
    val error by viewModel.error.collectAsState()
    val playlistFeedback by viewModel.playlistFeedback.collectAsState()
    val isRefreshing = isProfileLoading || isPostsLoading
    val snackbarHostState = remember { SnackbarHostState() }
    var isProfileImageViewerOpen by remember { mutableStateOf(false) }

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
                                ProfileAvatarMedia(
                                   modifier = Modifier.size(60.dp),
                                   profileImageUrl = profile?.profile_image_url,
                                    profileVideoUrl = profile?.profile_video_url,
                                    autoPlayVideo = true,
                                    onClick = {
                                        if (!profile?.profile_image_url.isNullOrBlank()) {
                                            isProfileImageViewerOpen = true
                                        }
                                    }
                               )

                                Column {
                                    Text(
                                        text = profile?.name?.takeIf { it.isNotBlank() }
                                            ?: profile?.username.orEmpty(),
                                        style = MaterialTheme.typography.titleLarge
                                    )
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
                                        Text(if (isFollowing) "Unfollow" else "Follow")
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
                                        text = "Followers"
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
                                        text = "Following"
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
                                    Text(text = "Posts")

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
                    text = "Posts",
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
                            text = "No posts yet",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Share your first moment with friends and start building your world.",
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
                                text = "Create your first post",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
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
                        selectedPostVote = viewModel.selectedPostVote(post.id),
                        postError = viewModel.postError(post.id),
                        onUpVoteClick = { viewModel.votePost(post.id, 1) },
                        onDownVoteClick = { viewModel.votePost(post.id, -1) },
                        onCommentsClick = { viewModel.openCommentsBottomSheet(post.id) },
                        onQuoteClick = { viewModel.openQuotePostOptions(post) },
                        onShareClick = { viewModel.openShareBottomSheet(post) },
                        onQuotedPostClick = viewModel::openQuotedPost,
                        onReportClick = if (isOwnProfile) null else {
                            { viewModel.openReportBottomSheet(post.id) }
                        },
                        onDeletePostClick = if (isOwnProfile) {
                            { viewModel.openDeletePostBottomSheet(post.id) }
                        } else {
                            null
                        },
                        onAddTrackToPlaylistClick = { media ->
                            viewModel.addTrackToPlaylist(media.id)
                        },
                        onShowTrackPlaylistAddersClick = { media, trackTitle ->
                            viewModel.openTrackPlaylistAddersDialog(media, trackTitle)
                        }
                    )
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
                    contentDescription = "Open settings"
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
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile image",
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
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}
