package com.dino.message.groupfeature.presentation.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.groupfeature.presentation.viewmodel.CreateGroupViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun CreateGroupScreen() {
    val viewModel: CreateGroupViewModel = hiltViewModel()
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
                title = { Text("Create Group") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateUp() }) {
                        Icon(Icons.Rounded.ArrowBackIosNew, "Back")
                    }
                },
                actions = {
                    if (isCreating) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 16.dp))
                    } else {
                        TextButton(onClick = { viewModel.createGroup() }) {
                            Text("Create")
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
                label = { Text("Group Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isCreating
            )

            Spacer(Modifier.height(DPMedium))

            OutlinedTextField(
                value = viewModel.searchQuery.value,
                onValueChange = { viewModel.searchQuery.value = it },
                label = { Text("Search followers") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isCreating
            )

            Spacer(Modifier.height(DPSmall))

            Text(
                text = "You can only add people who mutually follow you",
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
                            AsyncImage(
                                model = follower.profileImageUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(Modifier.width(DPMedium))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(follower.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "@${follower.username}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = "Selected",
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
