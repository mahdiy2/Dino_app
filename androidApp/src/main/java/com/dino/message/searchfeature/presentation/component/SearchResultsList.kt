package com.dino.message.searchfeature.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dino.message.R
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.searchfeature.domain.model.SearchTab
import com.dino.message.searchfeature.domain.model.SearchUser
import com.dino.message.threadfeature.domain.model.Post
import com.dino.message.threadfeature.domain.model.PostMedia
import com.dino.message.threadfeature.presentation.component.ThreadPostCard

@Composable
fun SearchResultsList(
    selectedTab: SearchTab,
    users: List<SearchUser>,
    posts: List<Post>,
    canDownloadPostMedia: Boolean,
    isLoading: Boolean,
    error: String?,
    query: String,
    onUserClick: (String) -> Unit,
    onUpVoteClick: (Long) -> Unit,
    onDownVoteClick: (Long) -> Unit,
    onCommentsClick: (Long) -> Unit,
    onQuoteClick: (Post) -> Unit,
    onShareClick: (Post) -> Unit,
    onPostLongClick: (Long) -> Unit,
    onQuotedPostClick: (Long) -> Unit,
    onReportClick: (Long) -> Unit,
    onAddTrackToPlaylistClick: (Long) -> Unit,
    onShowTrackPlaylistAddersClick: (postMedia: PostMedia, trackTitle: String) -> Unit,
    selectedPostVote: (Long) -> Long,
    postError: (Long) -> String?,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (error != null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }

    val hasUsers = users.isNotEmpty()
    val hasPosts = posts.isNotEmpty()
    val isEmpty = !hasUsers && !hasPosts

    if (isEmpty && query.isNotBlank()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.search_no_results),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    if (query.isBlank()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.search_prompt_users_posts),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(DPMedium)
    ) {
        if (hasUsers && (selectedTab == SearchTab.USERS || selectedTab == SearchTab.BOTH)) {
            if (selectedTab == SearchTab.BOTH) {
                item {
                    Text(
                        text = stringResource(R.string.users),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = DPMedium, vertical = DPMedium)
                    )
                }
            }
            items(
                items = users,
                key = { "user_${it.id}" }
            ) { user ->
                SearchUserItem(
                    user = user,
                    onClick = { onUserClick(user.username) }
                )
            }
        }

        if (hasPosts && (selectedTab == SearchTab.POSTS || selectedTab == SearchTab.BOTH)) {
            if (selectedTab == SearchTab.BOTH) {
                item {
                    Text(
                        text = stringResource(R.string.posts),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = DPMedium, vertical = DPMedium)
                    )
                }
            }
            items(
                items = posts,
                key = { "post_${it.id}" }
            ) { post ->
                ThreadPostCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DPMedium),
                    post = post,
                    canDownloadMedia = canDownloadPostMedia,
                    selectedPostVote = selectedPostVote(post.id),
                    postError = postError(post.id),
                    onUpVoteClick = { onUpVoteClick(post.id) },
                    onDownVoteClick = { onDownVoteClick(post.id) },
                    onCommentsClick = { onCommentsClick(post.id) },
                    onQuoteClick = { onQuoteClick(post) },
                    onShareClick = { onShareClick(post) },
                    onPostLongClick = onPostLongClick,
                    onQuotedPostClick = onQuotedPostClick,
                    onReportClick = { onReportClick(post.id) },
                    onUserClick = { onUserClick(post.author.username) },
                    onAddTrackToPlaylistClick = { media ->
                        onAddTrackToPlaylistClick(media.id)
                    },
                    onShowTrackPlaylistAddersClick = onShowTrackPlaylistAddersClick
                )
            }
        }
    }
}
