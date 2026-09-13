package com.dino.message.profilefeature.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dino.message.composeapp.generated.resources.Res
import com.dino.message.composeapp.generated.resources.back
import com.dino.message.composeapp.generated.resources.block_list
import com.dino.message.composeapp.generated.resources.block_list_empty
import com.dino.message.composeapp.generated.resources.refresh
import com.dino.message.composeapp.generated.resources.unblock
import com.dino.message.corefeature.presentation.component.UserBadgeIcon
import com.dino.message.corefeature.presentation.icons.AppIcons
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.profilefeature.domain.model.ProfileUserSummary
import com.dino.message.threadfeature.presentation.component.PostContainer
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockListContent(
    users: List<ProfileUserSummary>,
    isLoading: Boolean,
    error: String?,
    actionUsername: String?,
    snackbarHostState: SnackbarHostState,
    onNavigateUp: () -> Unit,
    onRefresh: () -> Unit,
    onUnblock: (String) -> Unit,
    avatarContent: @Composable (ProfileUserSummary, Modifier) -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(Res.string.block_list)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = AppIcons.ArrowBackIosNew,
                            contentDescription = stringResource(Res.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = AppIcons.Refresh,
                            contentDescription = stringResource(Res.string.refresh)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = DPExtraLarge, vertical = DPMedium),
            verticalArrangement = Arrangement.spacedBy(DPMedium)
        ) {
            if (error != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            modifier = Modifier.padding(DPMedium),
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (users.isEmpty()) {
                item {
                    Text(
                        text = stringResource(Res.string.block_list_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(
                    items = users,
                    key = { it.id }
                ) { user ->
                    PostContainer(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(DPLarge),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(DPMedium)
                        ) {
                            avatarContent(user, Modifier.size(48.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(DPSmall)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = user.name?.takeIf { it.isNotBlank() } ?: user.username,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    UserBadgeIcon(
                                        badge = user.badge,
                                        size = 17.dp
                                    )
                                }
                                Text(
                                    text = "@${user.username}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            FilledTonalButton(
                                onClick = { onUnblock(user.username) },
                                enabled = actionUsername == null
                            ) {
                                if (actionUsername == user.username) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(stringResource(Res.string.unblock))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
