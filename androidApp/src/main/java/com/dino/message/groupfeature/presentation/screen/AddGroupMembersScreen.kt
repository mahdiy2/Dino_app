package com.dino.message.groupfeature.presentation.screen

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.dino.message.corefeature.presentation.viewmodel.rememberStateHolder
import com.dino.message.groupfeature.presentation.viewmodel.AddGroupMembersViewModel
import org.koin.core.parameter.parametersOf

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

    AddGroupMembersContent(
        groupName = title,
        searchQuery = viewModel.searchQuery.value,
        followers = viewModel.filteredFollowers(),
        selectedUsernames = viewModel.selectedUsernames,
        isLoading = isLoading,
        isSubmitting = isSubmitting,
        onSearchQueryChange = { viewModel.searchQuery.value = it },
        onToggleMember = viewModel::toggleMember,
        onAddMembers = viewModel::addSelectedMembers,
        onNavigateUp = viewModel::navigateUp,
    )
}
