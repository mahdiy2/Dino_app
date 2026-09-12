package com.dino.message.groupfeature.presentation.screen

import android.widget.Toast
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dino.message.corefeature.presentation.viewmodel.rememberStateHolder
import com.dino.message.R
import com.dino.message.corefeature.presentation.component.UserBadgeIcon
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.corefeature.presentation.util.backIconModifier
import com.dino.message.profilefeature.presentation.component.ProfileAvatarMedia
import com.dino.message.groupfeature.presentation.viewmodel.AddGroupMembersViewModel
import com.dino.message.corefeature.presentation.icons.AppIcons
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGroupMembersScreen(
    groupId: Int,
    groupName: String? = null,
) {
    val viewModel: AddGroupMembersViewModel = rememberStateHolder(groupId, groupName) {
        parametersOf(groupId, groupName)
    }
    val context = LocalContext.current

    val title by viewModel.groupName.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val membersAdded by viewModel.membersAdded.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val followers = viewModel.filteredFollowers()

    val resolvedGroupName = title.ifBlank { groupName ?: stringResource(R.string.group_fallback_name) }

    LaunchedEffect(membersAdded) {
        if (membersAdded) {
            viewModel.navigateUp()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.group_add_members_title)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateUp() }) {
                        Icon(
                            imageVector = AppIcons.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.back),
                            modifier = backIconModifier()
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { viewModel.addSelectedMembers() },
                        enabled = !isSubmitting,
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                            )
                        } else {
                            Text(stringResource(R.string.add))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = DPMedium),
        ) {
            Spacer(modifier = Modifier.height(DPMedium))

            Text(
                text = resolvedGroupName,
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(modifier = Modifier.height(DPSmall))

            OutlinedTextField(
                value = viewModel.searchQuery.value,
                onValueChange = { viewModel.searchQuery.value = it },
                label = { Text(stringResource(R.string.search_followers)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isSubmitting,
                shape = MaterialTheme.shapes.extraLarge,
            )

            Spacer(modifier = Modifier.height(DPSmall))

            Text(
                text = stringResource(R.string.group_add_members_mutual_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(DPMedium))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                followers.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.group_add_members_empty),
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
                            val isSelected = viewModel.selectedUsernames.contains(follower.username)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isSubmitting) { viewModel.toggleMember(follower.username) }
                                    .padding(vertical = DPSmall),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ProfileAvatarMedia(
                                    profileImageUrl = follower.profileImageUrl,
                                    profileVideoUrl = null,
                                    badge = follower.badge,
                                    profileImageShape = follower.profileImageShape,
                                    modifier = Modifier.size(48.dp),
                                )
                                Spacer(modifier = Modifier.size(DPMedium))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = follower.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                        UserBadgeIcon(
                                            badge = follower.badge,
                                            size = 16.dp
                                        )
                                    }
                                    Text(
                                        text = "@${follower.username}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = AppIcons.Check,
                                        contentDescription = stringResource(R.string.selected),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
