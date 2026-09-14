package com.dino.message.searchfeature.presentation.screen

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.dino.message.corefeature.presentation.viewmodel.rememberStateHolder
import com.dino.message.searchfeature.presentation.viewmodel.SearchViewModel
import com.dino.message.threadfeature.presentation.component.ThreadPostCard

@Composable
fun SearchScreen() {
    val viewModel: SearchViewModel = rememberStateHolder()
    val query = viewModel.query.value
    val selectedTab = viewModel.selectedTab.value
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val viewerHasBadge by viewModel.viewerHasBadge.collectAsState()
    val playlistFeedback by viewModel.playlistFeedback.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    LaunchedEffect(playlistFeedback) {
        playlistFeedback?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearPlaylistFeedback()
        }
    }

    SearchContent(
        query = query,
        selectedTab = selectedTab,
        users = viewModel.users,
        posts = viewModel.posts,
        isLoading = isLoading,
        error = error,
        focusRequester = focusRequester,
        snackbarHostState = snackbarHostState,
        onQueryChange = viewModel::onQueryChange,
        onTabChange = viewModel::onTabChange,
        onBack = viewModel::navigateBack,
        onUserClick = viewModel::openUserProfile,
        postContent = { post ->
            ThreadPostCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = com.dino.message.corefeature.presentation.ui.theme.DPMedium),
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
                onReportClick = { viewModel.openReportBottomSheet(post.id) },
                onUserClick = { viewModel.openUserProfile(post.author.username) },
                onAddTrackToPlaylistClick = { viewModel.addTrackToPlaylist(it.id) },
                onShowTrackPlaylistAddersClick = viewModel::openTrackPlaylistAddersDialog,
            )
        },
    )
}
