package com.dino.message.profilefeature.presentation.screen

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.dino.message.corefeature.presentation.viewmodel.rememberStateHolder
import com.dino.message.profilefeature.presentation.component.ProfileAvatarMedia
import com.dino.message.profilefeature.presentation.viewmodel.BlockListViewModel

@Composable
fun BlockListScreen() {
    val viewModel: BlockListViewModel = rememberStateHolder()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val actionUsername by viewModel.actionUsername.collectAsState()
    val feedbackMessage by viewModel.feedbackMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearFeedbackMessage()
        }
    }

    BlockListContent(
        users = viewModel.users,
        isLoading = isLoading,
        error = error,
        actionUsername = actionUsername,
        snackbarHostState = snackbarHostState,
        onNavigateUp = viewModel::navigateUp,
        onRefresh = viewModel::refresh,
        onUnblock = viewModel::unblock,
        avatarContent = { user, modifier ->
            ProfileAvatarMedia(
                modifier = modifier,
                profileImageUrl = user.profile_image_url,
                profileVideoUrl = null,
                badge = user.badge,
                profileImageShape = user.profile_image_shape
            )
        },
    )
}
