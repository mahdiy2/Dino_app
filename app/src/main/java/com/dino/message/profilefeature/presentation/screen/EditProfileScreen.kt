package com.dino.message.profilefeature.presentation.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.profilefeature.presentation.viewmodel.EditProfileViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun EditProfileScreen() {
    val viewModel: EditProfileViewModel = hiltViewModel()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedImage = viewModel.selectedImage.value
    val selectedVideo = viewModel.selectedVideo.value
    val currentProfileImageUrl = viewModel.currentProfileImageUrl.value
    val currentProfileVideoUrl = viewModel.currentProfileVideoUrl.value
    val isFormEnabled = !isLoading && !isSaving

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        viewModel.onImagePicked(uri)
    }
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        viewModel.onVideoPicked(uri)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Edit Profile") },
                navigationIcon = {
                    IconButton(
                        onClick = viewModel::closeScreen,
                        enabled = !isSaving
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = viewModel::saveProfile,
                        enabled = isFormEnabled
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = DPExtraLarge, vertical = DPMedium),
            verticalArrangement = Arrangement.spacedBy(DPMedium)
        ) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = viewModel.nameDraft.value,
                onValueChange = viewModel::onNameChanged,
                label = { Text("Name") },
                enabled = isFormEnabled,
                shape = MaterialTheme.shapes.extraLarge
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = viewModel.bioDraft.value,
                onValueChange = viewModel::onBioChanged,
                label = { Text("Bio") },
                minLines = 2,
                maxLines = 6,
                enabled = isFormEnabled,
                shape = MaterialTheme.shapes.extraLarge
            )

            Text(
                text = "Profile media",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DPMedium)
            ) {
                FilledTonalButton(
                    onClick = { imagePickerLauncher.launch(arrayOf("image/*")) },
                    enabled = isFormEnabled
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AttachFile,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(DPMedium))
                    Text(text = if (selectedImage == null) "Pick image" else "Change image")
                }

                FilledTonalButton(
                    onClick = { videoPickerLauncher.launch(arrayOf("video/*")) },
                    enabled = isFormEnabled
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AttachFile,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(DPMedium))
                    Text(text = if (selectedVideo == null) "Pick video" else "Change video")
                }
            }

            if (selectedImage != null) {
                AssistChip(
                    onClick = viewModel::clearSelectedImage,
                    label = { Text("Image: ${selectedImage.displayName}") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear selected image"
                        )
                    }
                )
            } else if (currentProfileImageUrl != null) {
                Text(
                    text = "Current profile image is set",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (selectedVideo != null) {
                AssistChip(
                    onClick = viewModel::clearSelectedVideo,
                    label = { Text("Video: ${selectedVideo.displayName}") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear selected video"
                        )
                    }
                )
            } else if (currentProfileVideoUrl != null) {
                Text(
                    text = "Current profile video is set",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isLoading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            if (error != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        modifier = Modifier.padding(DPMedium),
                        text = error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}
