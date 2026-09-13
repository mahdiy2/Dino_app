package com.dino.message.groupfeature.presentation.screen

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.dino.message.corefeature.presentation.viewmodel.rememberStateHolder
import com.dino.message.groupfeature.presentation.viewmodel.CreateGroupViewModel
import com.dino.message.profilefeature.presentation.component.ProfileAvatarMedia

@Composable
fun CreateGroupScreen() {
    val viewModel: CreateGroupViewModel = rememberStateHolder()
    val context = LocalContext.current

    val isLoadingFollowers by viewModel.isLoadingFollowers.collectAsState()
    val isCreating by viewModel.isCreating.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val groupCreated by viewModel.groupCreated.collectAsState()

    LaunchedEffect(groupCreated) {
        if (groupCreated) viewModel.navigateUp()
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.dismissError()
        }
    }

    CreateGroupContent(
        groupName = viewModel.groupName.value,
        searchQuery = viewModel.searchQuery.value,
        followers = viewModel.filteredFollowers(),
        selectedUsernames = viewModel.selectedUsernames,
        isLoadingFollowers = isLoadingFollowers,
        isCreating = isCreating,
        onGroupNameChange = { viewModel.groupName.value = it },
        onSearchQueryChange = { viewModel.searchQuery.value = it },
        onToggleMember = viewModel::toggleMember,
        onCreateGroup = viewModel::createGroup,
        onNavigateUp = viewModel::navigateUp,
        avatarContent = { follower, modifier ->
            ProfileAvatarMedia(
                profileImageUrl = follower.profileImageUrl,
                profileVideoUrl = null,
                badge = follower.badge,
                profileImageShape = follower.profileImageShape,
                modifier = modifier,
            )
        },
    )
}
