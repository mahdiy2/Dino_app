package com.dino.message.groupfeature.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dino.message.composeapp.generated.resources.Res
import com.dino.message.composeapp.generated.resources.back
import com.dino.message.composeapp.generated.resources.close
import com.dino.message.composeapp.generated.resources.creator
import com.dino.message.composeapp.generated.resources.group_add_members_title
import com.dino.message.composeapp.generated.resources.group_member_profile_image
import com.dino.message.composeapp.generated.resources.group_members_empty
import com.dino.message.composeapp.generated.resources.group_members_manage_hint
import com.dino.message.composeapp.generated.resources.group_members_title
import com.dino.message.corefeature.presentation.component.UserBadgeIcon
import com.dino.message.corefeature.presentation.icons.AppIcons
import com.dino.message.corefeature.presentation.platform.LocalPlatformMediaRenderer
import com.dino.message.corefeature.presentation.platform.PlatformImageOptions
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.groupfeature.domain.model.GroupMember
import com.dino.message.profilefeature.presentation.component.ProfileAvatarMedia
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupMembersContent(
    groupName: String,
    members: List<GroupMember>,
    creatorUsername: String?,
    canManageMembers: Boolean,
    isLoading: Boolean,
    onNavigateUp: () -> Unit,
    onAddMembers: () -> Unit,
    onMemberClick: (String) -> Unit,
) {
    var selectedProfileImageUrl by remember { mutableStateOf<String?>(null) }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(Res.string.group_members_title, groupName)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(AppIcons.ArrowBackIosNew, stringResource(Res.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            if (canManageMembers) {
                FloatingActionButton(onClick = onAddMembers) {
                    Icon(AppIcons.Add, stringResource(Res.string.group_add_members_title))
                }
            }
        },
    ) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            members.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(Res.string.group_members_empty))
            }
            else -> Column(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = DPMedium),
            ) {
                if (!canManageMembers) {
                    Text(
                        stringResource(Res.string.group_members_manage_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = DPSmall),
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(DPSmall),
                ) {
                    items(members, key = { it.id }) { member ->
                        GroupMemberItem(
                            member = member,
                            isCreator = member.username == creatorUsername,
                            onAvatarClick = { selectedProfileImageUrl = member.profileImageUrl },
                            onMemberClick = { onMemberClick(member.username) },
                        )
                    }
                }
            }
        }
    }
    selectedProfileImageUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
        GroupMemberProfileImageDialog(imageUrl) { selectedProfileImageUrl = null }
    }
}

@Composable
private fun GroupMemberItem(
    member: GroupMember,
    isCreator: Boolean,
    onAvatarClick: () -> Unit,
    onMemberClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onMemberClick).padding(vertical = DPSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatarMedia(
            profileImageUrl = member.profileImageUrl,
            profileVideoUrl = null,
            badge = member.badge,
            profileImageShape = member.profileImageShape,
            modifier = Modifier.size(48.dp).clickable(onClick = onAvatarClick),
        )
        Spacer(Modifier.size(DPMedium))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(member.name, style = MaterialTheme.typography.bodyLarge)
                UserBadgeIcon(member.badge, size = 16.dp)
            }
            Text("@${member.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (isCreator) {
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(stringResource(Res.string.creator)) },
                leadingIcon = { Icon(AppIcons.Star, null) },
            )
        }
    }
}

@Composable
private fun GroupMemberProfileImageDialog(imageUrl: String, onDismiss: () -> Unit) {
    val renderer = LocalPlatformMediaRenderer.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            renderer.image(
                imageUrl,
                stringResource(Res.string.group_member_profile_image),
                Modifier.fillMaxSize(),
                ContentScale.Fit,
                PlatformImageOptions(enableDiskCache = true, enableMemoryCache = true),
                {},
            )
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                Icon(AppIcons.Close, stringResource(Res.string.close), tint = Color.White)
            }
        }
    }
}
