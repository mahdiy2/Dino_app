package com.dino.message.searchfeature.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.dino.message.R
import com.dino.message.corefeature.presentation.util.backIconModifier
import com.dino.message.searchfeature.presentation.component.SearchResultsList
import com.dino.message.searchfeature.presentation.component.SearchTabRow
import com.dino.message.searchfeature.presentation.viewmodel.SearchViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun SearchScreen() {
    val viewModel: SearchViewModel = hiltViewModel()
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

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = viewModel::onQueryChange,
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search_placeholder),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = viewModel::navigateBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.back),
                            modifier = backIconModifier()
                        )
                    }
                },
                actions = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = stringResource(R.string.clear)
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SearchTabRow(
                selectedTab = selectedTab,
                onTabSelected = viewModel::onTabChange
            )

            SearchResultsList(
                selectedTab = selectedTab,
                users = viewModel.users,
                posts = viewModel.posts,
                canDownloadPostMedia = viewerHasBadge,
                isLoading = isLoading,
                error = error,
                query = query,
                onUserClick = viewModel::openUserProfile,
                onUpVoteClick = { postId -> viewModel.votePost(postId, 1) },
                onDownVoteClick = { postId -> viewModel.votePost(postId, -1) },
                onCommentsClick = viewModel::openCommentsBottomSheet,
                onQuoteClick = viewModel::openQuotePostOptions,
                onShareClick = viewModel::openShareBottomSheet,
                onPostLongClick = viewModel::openQuotedPost,
                onQuotedPostClick = viewModel::openQuotedPost,
                onReportClick = viewModel::openReportBottomSheet,
                onAddTrackToPlaylistClick = viewModel::addTrackToPlaylist,
                onShowTrackPlaylistAddersClick = viewModel::openTrackPlaylistAddersDialog,
                selectedPostVote = viewModel::selectedPostVote,
                postError = viewModel::postError,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
