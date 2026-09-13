package com.dino.message.groupfeature.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dino.message.composeapp.generated.resources.Res
import com.dino.message.composeapp.generated.resources.add
import com.dino.message.composeapp.generated.resources.back
import com.dino.message.composeapp.generated.resources.create
import com.dino.message.composeapp.generated.resources.group_add_members_empty
import com.dino.message.composeapp.generated.resources.group_add_members_mutual_hint
import com.dino.message.composeapp.generated.resources.group_add_members_title
import com.dino.message.composeapp.generated.resources.group_create_mutual_followers_hint
import com.dino.message.composeapp.generated.resources.group_create_title
import com.dino.message.composeapp.generated.resources.group_name
import com.dino.message.composeapp.generated.resources.search_followers
import com.dino.message.composeapp.generated.resources.selected
import com.dino.message.corefeature.presentation.component.UserBadgeIcon
import com.dino.message.corefeature.presentation.icons.AppIcons
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.groupfeature.domain.model.MutualFollower
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupContent(
    groupName: String,
    searchQuery: String,
    followers: List<MutualFollower>,
    selectedUsernames: List<String>,
    isLoadingFollowers: Boolean,
    isCreating: Boolean,
    onGroupNameChange: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleMember: (String) -> Unit,
    onCreateGroup: () -> Unit,
    onNavigateUp: () -> Unit,
    avatarContent: @Composable (MutualFollower, Modifier) -> Unit,
) {
    GroupMemberPickerScaffold(
        title = stringResource(Res.string.group_create_title),
        actionLabel = stringResource(Res.string.create),
        isActionLoading = isCreating,
        isActionEnabled = true,
        onAction = onCreateGroup,
        onNavigateUp = onNavigateUp,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = DPMedium)
        ) {
            Spacer(Modifier.height(DPMedium))

            OutlinedTextField(
                value = groupName,
                onValueChange = onGroupNameChange,
                label = { Text(stringResource(Res.string.group_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isCreating,
                shape = MaterialTheme.shapes.extraLarge
            )

            Spacer(Modifier.height(DPMedium))

            GroupFollowerSearchAndList(
                searchQuery = searchQuery,
                followers = followers,
                selectedUsernames = selectedUsernames,
                isLoading = isLoadingFollowers,
                isSubmitting = isCreating,
                hint = stringResource(Res.string.group_create_mutual_followers_hint),
                emptyMessage = null,
                onSearchQueryChange = onSearchQueryChange,
                onToggleMember = onToggleMember,
                avatarContent = avatarContent,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGroupMembersContent(
    groupName: String,
    searchQuery: String,
    followers: List<MutualFollower>,
    selectedUsernames: List<String>,
    isLoading: Boolean,
    isSubmitting: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleMember: (String) -> Unit,
    onAddMembers: () -> Unit,
    onNavigateUp: () -> Unit,
    avatarContent: @Composable (MutualFollower, Modifier) -> Unit,
) {
    GroupMemberPickerScaffold(
        title = stringResource(Res.string.group_add_members_title),
        actionLabel = stringResource(Res.string.add),
        isActionLoading = isSubmitting,
        isActionEnabled = !isSubmitting,
        onAction = onAddMembers,
        onNavigateUp = onNavigateUp,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = DPMedium),
        ) {
            Spacer(modifier = Modifier.height(DPMedium))

            Text(
                text = groupName,
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(DPSmall))

            GroupFollowerSearchAndList(
                searchQuery = searchQuery,
                followers = followers,
                selectedUsernames = selectedUsernames,
                isLoading = isLoading,
                isSubmitting = isSubmitting,
                hint = stringResource(Res.string.group_add_members_mutual_hint),
                emptyMessage = stringResource(Res.string.group_add_members_empty),
                onSearchQueryChange = onSearchQueryChange,
                onToggleMember = onToggleMember,
                avatarContent = avatarContent,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupMemberPickerScaffold(
    title: String,
    actionLabel: String,
    isActionLoading: Boolean,
    isActionEnabled: Boolean,
    onAction: () -> Unit,
    onNavigateUp: () -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = AppIcons.ArrowBackIosNew,
                            contentDescription = stringResource(Res.string.back)
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = onAction,
                        enabled = isActionEnabled,
                    ) {
                        if (isActionLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Text(actionLabel)
                        }
                    }
                }
            )
        },
        content = content,
    )
}

@Composable
private fun GroupFollowerSearchAndList(
    searchQuery: String,
    followers: List<MutualFollower>,
    selectedUsernames: List<String>,
    isLoading: Boolean,
    isSubmitting: Boolean,
    hint: String,
    emptyMessage: String?,
    onSearchQueryChange: (String) -> Unit,
    onToggleMember: (String) -> Unit,
    avatarContent: @Composable (MutualFollower, Modifier) -> Unit,
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        label = { Text(stringResource(Res.string.search_followers)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = !isSubmitting,
        shape = MaterialTheme.shapes.extraLarge
    )

    Spacer(Modifier.height(DPSmall))

    Text(
        text = hint,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(Modifier.height(DPMedium))

    when {
        isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        emptyMessage != null && followers.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(DPSmall),
            ) {
                items(followers, key = { follower -> follower.id }) { follower ->
                    val isSelected = selectedUsernames.contains(follower.username)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isSubmitting) { onToggleMember(follower.username) }
                            .padding(vertical = DPSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        avatarContent(follower, Modifier.size(48.dp))
                        Spacer(Modifier.size(DPMedium))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(follower.name, style = MaterialTheme.typography.bodyLarge)
                                UserBadgeIcon(
                                    badge = follower.badge,
                                    size = 16.dp
                                )
                            }
                            Text(
                                "@${follower.username}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isSelected) {
                            Icon(
                                AppIcons.Check,
                                contentDescription = stringResource(Res.string.selected),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
