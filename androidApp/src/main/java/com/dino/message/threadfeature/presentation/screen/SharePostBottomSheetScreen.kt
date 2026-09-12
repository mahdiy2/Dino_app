package com.dino.message.threadfeature.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dino.message.corefeature.presentation.viewmodel.rememberStateHolder
import com.dino.message.R
import com.dino.message.corefeature.presentation.component.UserBadgeIcon
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.profilefeature.presentation.component.ProfileAvatarMedia
import com.dino.message.threadfeature.presentation.viewmodel.SharePostBottomSheetViewModel
import com.dino.message.corefeature.presentation.icons.AppIcons

@Composable
fun SharePostBottomSheetScreen(
    postId: Long,
    authorUsername: String,
    postText: String,
) {
    val viewModel: SharePostBottomSheetViewModel = rememberStateHolder(postId, authorUsername, postText)
    val isLoading by viewModel.isRecipientsLoading.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedCount = viewModel.selectedCount

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(DPMedium)
            .windowInsetsPadding(WindowInsets.navigationBars),
        verticalArrangement = Arrangement.spacedBy(DPMedium)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier.padding(DPMedium),
                verticalArrangement = Arrangement.spacedBy(DPSmall)
            ) {
                Text(
                    text = stringResource(R.string.share_post_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.share_post_header, postId, authorUsername),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = if (selectedCount > 0) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.selected_count, selectedCount),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selectedCount > 0) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                if (postText.isNotBlank()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            modifier = Modifier.padding(DPMedium),
                            text = postText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            if (isLoading && viewModel.contacts.isEmpty() && viewModel.groups.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = DPMedium),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 220.dp, max = 420.dp)
                        .padding(horizontal = DPSmall, vertical = DPSmall),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (viewModel.groups.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.groups),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    start = DPSmall,
                                    top = DPSmall,
                                    bottom = 4.dp
                                )
                            )
                        }
                        items(viewModel.groups, key = { "group_${it.id}" }) { group ->
                            val selected = viewModel.isGroupSelected(group.id)
                            SelectableRecipientRow(
                                title = group.name,
                                subtitle = stringResource(R.string.group_members_count, group.memberCount),
                                selected = selected,
                                onClick = { viewModel.toggleGroup(group.id) },
                                leading = {
                                    Icon(
                                        imageVector = AppIcons.Group,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                    }

                    if (viewModel.contacts.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.contacts),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(
                                    start = DPSmall,
                                    top = DPMedium,
                                    bottom = 4.dp
                                )
                            )
                        }
                        items(viewModel.contacts, key = { "contact_${it.username}" }) { contact ->
                            val selected = viewModel.isContactSelected(contact.username)
                            SelectableRecipientRow(
                                title = contact.displayName,
                                subtitle = "@${contact.username}",
                                badge = contact.badge,
                                selected = selected,
                                onClick = { viewModel.toggleContact(contact.username) },
                                leading = {
                                    ProfileAvatarMedia(
                                        profileImageUrl = contact.profileImageUrl,
                                        badge = contact.badge,
                                        profileImageShape = contact.profileImageShape,
                                        profileVideoUrl = null,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        if (error != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    modifier = Modifier.padding(DPSmall),
                    text = error.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        FilledTonalButton(
            onClick = { viewModel.sendPostToSelectedRecipients(postId, authorUsername, postText) },
            enabled = !isSending && selectedCount > 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(stringResource(R.string.sending))
            } else {
                Text(stringResource(R.string.send_to_count, selectedCount))
            }
        }
    }
}

@Composable
private fun SelectableRecipientRow(
    title: String,
    subtitle: String,
    badge: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
    leading: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = DPMedium, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DPMedium)
        ) {
            leading()
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    )
                    UserBadgeIcon(
                        badge = badge,
                        size = 16.dp
                    )
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    imageVector = AppIcons.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
