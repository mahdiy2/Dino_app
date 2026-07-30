package com.dino.message.groupfeature.presentation.screen

import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.dino.message.R
import com.dino.message.corefeature.presentation.component.UserBadgeIcon
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.corefeature.presentation.util.backIconModifier
import com.dino.message.groupfeature.domain.model.GroupMember
import com.dino.message.groupfeature.presentation.viewmodel.GroupMembersViewModel
import com.dino.message.profilefeature.presentation.component.ProfileAvatarMedia
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun GroupMembersScreen(
    groupId: Int,
    groupName: String? = null,
) {
    val viewModel: GroupMembersViewModel = koinViewModel()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val members by viewModel.members.collectAsState()
    val title by viewModel.groupName.collectAsState()
    val creatorUsername by viewModel.creatorUsername.collectAsState()
    val canManageMembers by viewModel.canManageMembers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var selectedProfileImageUrl by remember { mutableStateOf<String?>(null) }

    val resolvedGroupName = title.ifBlank { groupName ?: stringResource(R.string.group_fallback_name) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.dismissError()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadMembers()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.group_members_title, resolvedGroupName)) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.back),
                            modifier = backIconModifier()
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (canManageMembers) {
                FloatingActionButton(
                    onClick = { viewModel.openAddMembersScreen() },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.group_add_members_title))
                }
            }
        },
    ) { innerPadding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            members.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.group_members_empty),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = DPMedium),
                ) {
                    if (!canManageMembers) {
                        Text(
                            text = stringResource(R.string.group_members_manage_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = DPSmall),
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(DPSmall),
                    ) {
                        items(
                            items = members,
                            key = { member -> member.id },
                        ) { member ->
                            GroupMemberItem(
                                member = member,
                                isCreator = member.username == creatorUsername,
                                onAvatarClick = { profileImageUrl ->
                                    if (!profileImageUrl.isNullOrBlank()) {
                                        selectedProfileImageUrl = profileImageUrl
                                    }
                                },
                                onMemberClick = {
                                    viewModel.openMemberProfile(member.username)
                                }
                            )
                        }
                    }
                }
            }
        }

        if (!selectedProfileImageUrl.isNullOrBlank()) {
            GroupMemberProfileImageDialog(
                imageUrl = selectedProfileImageUrl!!,
                onDismiss = { selectedProfileImageUrl = null }
            )
        }
    }
}

@Composable
private fun GroupMemberItem(
    member: GroupMember,
    isCreator: Boolean,
    onAvatarClick: (String?) -> Unit,
    onMemberClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onMemberClick)
            .padding(vertical = DPSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProfileAvatarMedia(
            profileImageUrl = member.profileImageUrl,
            profileVideoUrl = null,
            badge = member.badge,
            profileImageShape = member.profileImageShape,
            modifier = Modifier
                .size(48.dp)
                .clickable {
                    onAvatarClick(member.profileImageUrl)
                },
        )

        Spacer(modifier = Modifier.size(DPMedium))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.bodyLarge,
                )
                UserBadgeIcon(
                    badge = member.badge,
                    size = 16.dp
                )
            }
            Text(
                text = "@${member.username}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (isCreator) {
            AssistChip(
                onClick = { },
                enabled = false,
                label = { Text(stringResource(R.string.creator)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                    )
                },
            )
        }
    }
}

@Composable
private fun GroupMemberProfileImageDialog(
    imageUrl: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.group_member_profile_image),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = Color.White,
                )
            }
        }
    }
}
