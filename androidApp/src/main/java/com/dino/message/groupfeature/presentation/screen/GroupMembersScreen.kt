package com.dino.message.groupfeature.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dino.message.R
import com.dino.message.corefeature.presentation.platform.LocalPlatformUiEffect
import com.dino.message.corefeature.presentation.viewmodel.rememberStateHolder
import com.dino.message.groupfeature.presentation.viewmodel.GroupMembersViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GroupMembersScreen(
    groupId: Int,
    groupName: String? = null,
) {
    val viewModel: GroupMembersViewModel = rememberStateHolder(groupId, groupName) {
        parametersOf(groupId, groupName)
    }
    val context = LocalContext.current
    val platformUiEffect = LocalPlatformUiEffect.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val members by viewModel.members.collectAsState()
    val title by viewModel.groupName.collectAsState()
    val creatorUsername by viewModel.creatorUsername.collectAsState()
    val canManageMembers by viewModel.canManageMembers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val resolvedGroupName = title.ifBlank { groupName ?: context.getString(R.string.group_fallback_name) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            platformUiEffect.showToast(it)
            viewModel.dismissError()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadMembers()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    GroupMembersContent(
        groupName = resolvedGroupName,
        members = members,
        creatorUsername = creatorUsername,
        canManageMembers = canManageMembers,
        isLoading = isLoading,
        onNavigateUp = viewModel::navigateUp,
        onAddMembers = viewModel::openAddMembersScreen,
        onMemberClick = viewModel::openMemberProfile,
    )
}
