package com.dino.message.threadfeature.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dino.message.corefeature.presentation.viewmodel.rememberStateHolder
import com.dino.message.R
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.corefeature.presentation.util.backIconModifier
import com.dino.message.threadfeature.presentation.component.ThreadCommentsSection
import com.dino.message.threadfeature.presentation.component.ThreadCommentsSectionStyle
import com.dino.message.threadfeature.presentation.component.ThreadPostCard
import com.dino.message.threadfeature.presentation.viewmodel.SharedPostDetailsViewModel
import com.dino.message.threadfeature.presentation.viewmodel.ThreadCommentsViewModel
import com.dino.message.corefeature.presentation.icons.AppIcons
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedPostDetailsScreen(
    postId: Long
) {
    val viewModel: SharedPostDetailsViewModel = rememberStateHolder(postId) {
        parametersOf(postId)
    }
    val commentsViewModel: ThreadCommentsViewModel = rememberStateHolder(postId)
    val post by viewModel.post.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedVote by viewModel.selectedVote.collectAsState()
    val viewerHasBadge by viewModel.viewerHasBadge.collectAsState()
    val playlistFeedback by viewModel.playlistFeedback.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(playlistFeedback) {
        playlistFeedback?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearPlaylistFeedback()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.post)) },
                navigationIcon = {
                    IconButton(onClick = viewModel::navigateBack) {
                        Icon(
                            imageVector = AppIcons.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.back),
                            modifier = backIconModifier()
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading && post == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            post != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.spacedBy(DPSmall)
                ) {
                    item {
                        ThreadPostCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = DPMedium, vertical = DPSmall),
                            post = post!!,
                            canDownloadMedia = viewerHasBadge,
                            selectedPostVote = selectedVote,
                            postError = error,
                            onUpVoteClick = { viewModel.votePost(1) },
                            onDownVoteClick = { viewModel.votePost(-1) },
                            onCommentsClick = viewModel::openCommentsBottomSheet,
                            onQuoteClick = viewModel::openQuotePostOptions,
                            onShareClick = viewModel::openShareBottomSheet,
                            onQuotedPostClick = viewModel::openQuotedPost,
                            onUserClick = { viewModel.openUserProfile(post!!.author.username) },
                            onAddTrackToPlaylistClick = { media ->
                                viewModel.addTrackToPlaylist(media.id)
                            },
                            onShowTrackPlaylistAddersClick = { media, trackTitle ->
                                viewModel.openTrackPlaylistAddersDialog(media, trackTitle)
                            }
                        )
                    }

                    item {
                        ThreadCommentsSection(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = DPMedium, vertical = DPSmall),
                            viewModel = commentsViewModel,
                            style = ThreadCommentsSectionStyle.Embedded
                        )
                    }
                }
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(DPMedium)
                    ) {
                        Text(
                            text = error ?: stringResource(R.string.post_not_found),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = viewModel::refresh) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
        }
    }
}
