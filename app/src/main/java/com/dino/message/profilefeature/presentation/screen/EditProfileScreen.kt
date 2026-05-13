package com.dino.message.profilefeature.presentation.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledIconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dino.message.R
import com.dino.message.corefeature.presentation.util.PROFILE_IMAGE_SHAPE_CIRCLE
import com.dino.message.corefeature.presentation.util.PROFILE_IMAGE_SHAPE_COOKIE_12_SIDED
import com.dino.message.corefeature.presentation.util.PROFILE_IMAGE_SHAPE_COOKIE_9_SIDED
import com.dino.message.corefeature.presentation.util.PROFILE_IMAGE_SHAPE_OPTIONS
import com.dino.message.corefeature.presentation.util.PROFILE_IMAGE_SHAPE_PILL
import com.dino.message.corefeature.presentation.util.hasBadgeAccess
import com.dino.message.corefeature.presentation.util.normalizeProfileImageShape
import com.dino.message.corefeature.presentation.util.resolveProfileImageShape
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.util.backIconModifier
import com.dino.message.profilefeature.presentation.viewmodel.EditProfileViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import coil.compose.SubcomposeAsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis

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
    val selectedProfileImageShape = viewModel.selectedProfileImageShape.value
    val currentProfileBadge = viewModel.currentProfileBadge.value
    val isFormEnabled = !isLoading && !isSaving
    val imagePreviewModel: Any? = selectedImage?.uri ?: currentProfileImageUrl?.takeIf { it.isNotBlank() }
    val videoPreviewModel: Any? = selectedVideo?.uri ?: currentProfileVideoUrl?.takeIf { it.isNotBlank() }
    val shapeAccessAvailable = hasBadgeAccess(currentProfileBadge)
    val resolvedAvatarShape = resolveProfileImageShape(
        badge = currentProfileBadge,
        profileImageShape = selectedProfileImageShape
    )

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
                title = { Text(text = stringResource(R.string.edit_profile)) },
                navigationIcon = {
                    IconButton(
                        onClick = viewModel::closeScreen,
                        enabled = !isSaving
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.back),
                            modifier = backIconModifier()
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
                            Text(stringResource(R.string.save))
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

            Text(
                text = stringResource(R.string.profile_media),
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DPMedium)
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box {
                        ProfileMediaPickerCircle(
                            previewModel = imagePreviewModel,
                            isVideoPreview = false,
                            shape = resolvedAvatarShape,
                            enabled = isFormEnabled,
                            onClick = { imagePickerLauncher.launch(arrayOf("image/*")) }
                        )
                        if (imagePreviewModel != null) {
                            FilledIconButton(
                                onClick = viewModel::removeProfileImage,
                                enabled = isFormEnabled,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(30.dp),
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.remove_profile_image),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.size(DPMedium))
                    Text(
                        text = if (imagePreviewModel == null) {
                            stringResource(R.string.pick_image)
                        } else {
                            stringResource(R.string.change_image)
                        }
                    )
                }



                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box {
                        ProfileMediaPickerCircle(
                            previewModel = videoPreviewModel,
                            isVideoPreview = true,
                            shape = resolvedAvatarShape,
                            enabled = isFormEnabled,
                            onClick = { videoPickerLauncher.launch(arrayOf("video/*")) }
                        )
                        if (videoPreviewModel != null) {
                            FilledIconButton(
                                onClick = viewModel::removeProfileVideo,
                                enabled = isFormEnabled,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(30.dp),
                                shape = CircleShape
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.remove_profile_video),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.size(DPMedium))
                    Text(
                        text = if (videoPreviewModel == null) {
                            stringResource(R.string.pick_video)
                        } else {
                            stringResource(R.string.change_video)
                        }
                    )
                }
            }


            Spacer(Modifier.size(DPMedium))


            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = viewModel.nameDraft.value,
                onValueChange = viewModel::onNameChanged,
                label = { Text(stringResource(R.string.name)) },
                enabled = isFormEnabled,
                shape = MaterialTheme.shapes.extraLarge
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = viewModel.bioDraft.value,
                onValueChange = viewModel::onBioChanged,
                label = { Text(stringResource(R.string.bio)) },
                minLines = 2,
                maxLines = 6,
                enabled = isFormEnabled,
                shape = MaterialTheme.shapes.extraLarge
            )

            if (shapeAccessAvailable) {
                Text(
                    text = stringResource(R.string.profile_picture_shape_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.profile_picture_shape_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(PROFILE_IMAGE_SHAPE_OPTIONS, key = { it }) { option ->
                        ProfileShapeOptionChip(
                            option = option,
                            selected = normalizeProfileImageShape(selectedProfileImageShape) == option,
                            enabled = isFormEnabled,
                            onClick = { viewModel.onProfileImageShapeSelected(option) }
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.profile_picture_shape_more_coming),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }



            Spacer(Modifier.size(DPExtraLarge))



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

@Composable
private fun ProfileMediaPickerCircle(
    previewModel: Any?,
    isVideoPreview: Boolean,
    shape: Shape,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.size(120.dp),
        shape = shape,
        onClick = onClick,
        enabled = enabled
    ) {
        if (previewModel == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            return@Card
        }

        val context = LocalContext.current
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(previewModel)
                .apply {
                    if (isVideoPreview) {
                        decoderFactory(VideoFrameDecoder.Factory())
                        videoFrameMillis(0)
                    }
                }
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            },
            error = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
        )
    }
}

@Composable
private fun ProfileShapeOptionChip(
    option: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val label = when (option) {
        PROFILE_IMAGE_SHAPE_CIRCLE -> stringResource(R.string.profile_picture_shape_circle)
        PROFILE_IMAGE_SHAPE_PILL -> stringResource(R.string.profile_picture_shape_pill)
        PROFILE_IMAGE_SHAPE_COOKIE_12_SIDED -> stringResource(R.string.profile_picture_shape_cookie_12)
        PROFILE_IMAGE_SHAPE_COOKIE_9_SIDED -> stringResource(R.string.profile_picture_shape_cookie_9)
        else -> option
    }
    val previewShape = resolveProfileImageShape(
        badge = "preview",
        profileImageShape = option
    )

    FilterChip(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        label = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .size(width = 26.dp, height = 18.dp),
                    shape = previewShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 0.9f else 0.5f)
                ) {}
                Text(text = label)
            }
        }
    )
}
