package com.dino.message.groupfeature.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.dino.message.R
import com.dino.message.corefeature.presentation.component.UserBadgeIcon
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.corefeature.presentation.util.backIconModifier
import com.dino.message.groupfeature.presentation.viewmodel.CreateGroupViewModel
import com.dino.message.profilefeature.presentation.component.ProfileAvatarMedia

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen() {
    val viewModel: CreateGroupViewModel = koinViewModel()
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
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.group_create_title)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.back),
                            modifier = backIconModifier()
                        )
                    }
                },
                actions = {
                    FilledTonalButton(onClick = { viewModel.createGroup() }) {
                        if (isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 16.dp)
                            )
                        } else {
                            Text(stringResource(R.string.create))

                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = DPMedium)
        ) {
            Spacer(Modifier.height(DPMedium))

            OutlinedTextField(
                value = viewModel.groupName.value,
                onValueChange = { viewModel.groupName.value = it },
                label = { Text(stringResource(R.string.group_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isCreating,
                shape = MaterialTheme.shapes.extraLarge
            )

            Spacer(Modifier.height(DPMedium))

            OutlinedTextField(
                value = viewModel.searchQuery.value,
                onValueChange = { viewModel.searchQuery.value = it },
                label = { Text(stringResource(R.string.search_followers)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isCreating,
                shape = MaterialTheme.shapes.extraLarge
            )

            Spacer(Modifier.height(DPSmall))

            Text(
                text = stringResource(R.string.group_create_mutual_followers_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(DPMedium))

            if (isLoadingFollowers) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(viewModel.filteredFollowers()) { follower ->
                        val isSelected = viewModel.selectedUsernames.contains(follower.username)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isCreating) { viewModel.toggleMember(follower.username) }
                                .padding(vertical = DPSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProfileAvatarMedia(
                                profileImageUrl = follower.profileImageUrl,
                                profileVideoUrl = null,
                                badge = follower.badge,
                                profileImageShape = follower.profileImageShape,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.width(DPMedium))
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
                                    Icons.Rounded.Check,
                                    contentDescription = stringResource(R.string.selected),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
