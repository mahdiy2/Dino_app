package com.dino.message.searchfeature.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import com.dino.message.composeapp.generated.resources.Res
import com.dino.message.composeapp.generated.resources.back
import com.dino.message.composeapp.generated.resources.clear
import com.dino.message.composeapp.generated.resources.search_placeholder
import com.dino.message.corefeature.presentation.icons.AppIcons
import com.dino.message.searchfeature.domain.model.SearchTab
import com.dino.message.searchfeature.domain.model.SearchUser
import com.dino.message.searchfeature.presentation.component.SearchResultsList
import com.dino.message.searchfeature.presentation.component.SearchTabRow
import com.dino.message.threadfeature.domain.model.Post
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchContent(
    query: String,
    selectedTab: SearchTab,
    users: List<SearchUser>,
    posts: List<Post>,
    isLoading: Boolean,
    error: String?,
    focusRequester: FocusRequester,
    snackbarHostState: SnackbarHostState,
    onQueryChange: (String) -> Unit,
    onTabChange: (SearchTab) -> Unit,
    onBack: () -> Unit,
    onUserClick: (String) -> Unit,
    postContent: @Composable (Post) -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = { Text(stringResource(Res.string.search_placeholder)) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.ArrowBackIosNew, stringResource(Res.string.back))
                    }
                },
                actions = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(AppIcons.Clear, stringResource(Res.string.clear))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            SearchTabRow(selectedTab, onTabChange)
            SearchResultsList(
                selectedTab = selectedTab,
                users = users,
                posts = posts,
                isLoading = isLoading,
                error = error,
                query = query,
                onUserClick = onUserClick,
                postContent = postContent,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
