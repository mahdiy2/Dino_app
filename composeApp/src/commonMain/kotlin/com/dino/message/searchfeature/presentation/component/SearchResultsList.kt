package com.dino.message.searchfeature.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dino.message.composeapp.generated.resources.Res
import com.dino.message.composeapp.generated.resources.posts
import com.dino.message.composeapp.generated.resources.search_no_results
import com.dino.message.composeapp.generated.resources.search_prompt_users_posts
import com.dino.message.composeapp.generated.resources.users
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.searchfeature.domain.model.SearchTab
import com.dino.message.searchfeature.domain.model.SearchUser
import com.dino.message.threadfeature.domain.model.Post
import org.jetbrains.compose.resources.stringResource

@Composable
fun SearchResultsList(
    selectedTab: SearchTab,
    users: List<SearchUser>,
    posts: List<Post>,
    isLoading: Boolean,
    error: String?,
    query: String,
    onUserClick: (String) -> Unit,
    postContent: @Composable (Post) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        isLoading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        error != null -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        query.isBlank() -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.search_prompt_users_posts), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        users.isEmpty() && posts.isEmpty() -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(Res.string.search_no_results), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(DPMedium),
        ) {
            if (users.isNotEmpty() && selectedTab in listOf(SearchTab.USERS, SearchTab.BOTH)) {
                if (selectedTab == SearchTab.BOTH) item {
                    Text(
                        stringResource(Res.string.users),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(DPMedium),
                    )
                }
                items(users, key = { "user_${it.id}" }) { user ->
                    SearchUserItem(user = user, onClick = { onUserClick(user.username) })
                }
            }
            if (posts.isNotEmpty() && selectedTab in listOf(SearchTab.POSTS, SearchTab.BOTH)) {
                if (selectedTab == SearchTab.BOTH) item {
                    Text(
                        stringResource(Res.string.posts),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(DPMedium),
                    )
                }
                items(posts, key = { "post_${it.id}" }) { post -> postContent(post) }
            }
        }
    }
}
