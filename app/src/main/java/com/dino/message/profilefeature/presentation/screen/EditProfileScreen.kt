package com.dino.message.profilefeature.presentation.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import org.koin.compose.viewmodel.koinViewModel
import com.dino.message.R
import com.dino.message.corefeature.presentation.platform.PlatformImageRequestOptions
import com.dino.message.corefeature.presentation.platform.PlatformSubcomposeAsyncImage
import com.dino.message.corefeature.presentation.platform.rememberPlatformImageRequest
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.corefeature.presentation.util.PROFILE_IMAGE_SHAPE_CIRCLE
import com.dino.message.corefeature.presentation.util.PROFILE_IMAGE_SHAPE_COOKIE_12_SIDED
import com.dino.message.corefeature.presentation.util.PROFILE_IMAGE_SHAPE_COOKIE_9_SIDED
import com.dino.message.corefeature.presentation.util.PROFILE_IMAGE_SHAPE_OPTIONS
import com.dino.message.corefeature.presentation.util.PROFILE_IMAGE_SHAPE_PILL
import com.dino.message.corefeature.presentation.util.backIconModifier
import com.dino.message.corefeature.presentation.util.hasBadgeAccess
import com.dino.message.corefeature.presentation.util.normalizeProfileImageShape
import com.dino.message.corefeature.presentation.util.resolveProfileImageShape
import com.dino.message.profilefeature.presentation.viewmodel.EditProfileViewModel
import com.dino.message.threadfeature.presentation.component.PostContainer
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen() {
    val viewModel: EditProfileViewModel = koinViewModel()
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
    val hasPendingMedia = selectedImage != null ||
        selectedVideo != null ||
        viewModel.removeProfileImageRequested.value ||
        viewModel.removeProfileVideoRequested.value
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

    val listState = rememberLazyListState()
    val topBarCollapsedFraction by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                // Keep collapse completion reachable with short content.
                (listState.firstVisibleItemScrollOffset / 140f).coerceIn(0f, 1f)
            }
        }
    }

    Scaffold(
        topBar = {
            EditProfileCollapsingTopBar(
                name = viewModel.nameDraft.value,
                bio = viewModel.bioDraft.value,
                isLoading = isLoading,
                isSaving = isSaving,
                hasPendingMedia = hasPendingMedia,
                previewModel = imagePreviewModel,
                shape = resolvedAvatarShape,
                collapsedFraction = topBarCollapsedFraction,
                onNavigateBack = viewModel::closeScreen,
                navigationEnabled = !isSaving,
                onSave = viewModel::saveProfile,
                saveEnabled = isFormEnabled
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            EditProfileAnimatedBackdrop(modifier = Modifier.fillMaxSize())

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(
                    horizontal = DPExtraLarge,
                    vertical = DPLarge
                ),
                verticalArrangement = Arrangement.spacedBy(DPLarge)
            ) {
                item("media") {
                    EditProfileSectionReveal(delayMillis = 70) {
                        PostContainer(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(DPExtraLarge),
                                verticalArrangement = Arrangement.spacedBy(DPLarge)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(DPMedium),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Icon(
                                            modifier = Modifier.padding(8.dp),
                                            imageVector = Icons.Rounded.CameraAlt,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(DPSmall)) {
                                        Text(
                                            text = stringResource(R.string.profile_media),
                                            style = MaterialTheme.typography.titleLarge
                                        )
                                        Text(
                                            text = stringResource(R.string.edit_profile_media_hint),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(DPMedium)
                                ) {
                                    ProfileMediaPickerCard(
                                        modifier = Modifier.weight(1f),
                                        title = if (imagePreviewModel == null) {
                                            stringResource(R.string.pick_image)
                                        } else {
                                            stringResource(R.string.change_image)
                                        },
                                        subtitle = stringResource(R.string.edit_profile_photo_hint),
                                        previewModel = imagePreviewModel,
                                        previewShape = resolvedAvatarShape,
                                        isVideoPreview = false,
                                        enabled = isFormEnabled,
                                        icon = Icons.Rounded.Image,
                                        onClick = { imagePickerLauncher.launch(arrayOf("image/*")) },
                                        onRemove = if (imagePreviewModel != null) viewModel::removeProfileImage else null
                                    )

                                    ProfileMediaPickerCard(
                                        modifier = Modifier.weight(1f),
                                        title = if (videoPreviewModel == null) {
                                            stringResource(R.string.pick_video)
                                        } else {
                                            stringResource(R.string.change_video)
                                        },
                                        subtitle = stringResource(R.string.edit_profile_video_hint),
                                        previewModel = videoPreviewModel,
                                        previewShape = resolvedAvatarShape,
                                        isVideoPreview = true,
                                        enabled = isFormEnabled,
                                        icon = Icons.Rounded.Movie,
                                        onClick = { videoPickerLauncher.launch(arrayOf("video/*")) },
                                        onRemove = if (videoPreviewModel != null) viewModel::removeProfileVideo else null
                                    )
                                }
                            }
                        }
                    }
                }

                item("identity") {
                    EditProfileSectionReveal(delayMillis = 125) {
                        PostContainer(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(DPExtraLarge),
                                verticalArrangement = Arrangement.spacedBy(DPMedium)
                            ) {
                                Text(
                                    text = stringResource(R.string.edit_profile_identity_title),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = stringResource(R.string.edit_profile_identity_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    modifier = Modifier.fillMaxWidth(),
                                    value = viewModel.nameDraft.value,
                                    onValueChange = viewModel::onNameChanged,
                                    label = { Text(stringResource(R.string.name)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Edit,
                                            contentDescription = null
                                        )
                                    },
                                    supportingText = {
                                        Text(
                                            text = stringResource(
                                                R.string.edit_profile_character_counter,
                                                viewModel.nameDraft.value.length,
                                                40
                                            )
                                        )
                                    },
                                    enabled = isFormEnabled,
                                    singleLine = true,
                                    shape = MaterialTheme.shapes.extraLarge
                                )

                                OutlinedTextField(
                                    modifier = Modifier.fillMaxWidth(),
                                    value = viewModel.bioDraft.value,
                                    onValueChange = viewModel::onBioChanged,
                                    label = { Text(stringResource(R.string.bio)) },
                                    supportingText = {
                                        Text(
                                            text = stringResource(
                                                R.string.edit_profile_character_counter,
                                                viewModel.bioDraft.value.length,
                                                140
                                            )
                                        )
                                    },
                                    minLines = 3,
                                    maxLines = 6,
                                    enabled = isFormEnabled,
                                    shape = MaterialTheme.shapes.extraLarge
                                )
                            }
                        }
                    }
                }

                if (shapeAccessAvailable) {
                    item("shape") {
                        EditProfileSectionReveal(delayMillis = 170) {
                            PostContainer(
                                modifier = Modifier.fillMaxWidth(),
                                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(DPExtraLarge),
                                    verticalArrangement = Arrangement.spacedBy(DPMedium)
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(DPMedium),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.tertiaryContainer
                                        ) {
                                            Icon(
                                                modifier = Modifier.padding(8.dp),
                                                imageVector = Icons.Rounded.Star,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(DPSmall)) {
                                            Text(
                                                text = stringResource(R.string.profile_picture_shape_title),
                                                style = MaterialTheme.typography.titleLarge
                                            )
                                            Text(
                                                text = stringResource(R.string.profile_picture_shape_subtitle),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

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
                            }
                        }
                    }
                }

                if (isLoading) {
                    item("loading") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }

                if (error != null) {
                    item("error") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.large
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
    }
}

@Composable
private fun EditProfileAnimatedBackdrop(modifier: Modifier = Modifier) {
    val primaryBlobColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f)
    val secondaryBlobColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
    val transition = rememberInfiniteTransition(label = "edit_profile_background")
    val firstBlobProgress by transition.animateFloat(
        initialValue = 0.05f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 13_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "edit_profile_blob_1"
    )
    val secondBlobProgress by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = -0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "edit_profile_blob_2"
    )

    Canvas(modifier = modifier) {
        drawCircle(
            color = primaryBlobColor,
            radius = size.minDimension * 0.42f,
            center = Offset(
                x = size.width * firstBlobProgress,
                y = size.height * 0.15f
            )
        )
        drawCircle(
            color = secondaryBlobColor,
            radius = size.minDimension * 0.36f,
            center = Offset(
                x = size.width * secondBlobProgress,
                y = size.height * 0.84f
            )
        )
    }
}

@Composable
private fun EditProfileSectionReveal(
    delayMillis: Int,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "edit_profile_section_alpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 28f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "edit_profile_section_offset"
    )

    Box(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha
            translationY = offsetY
        }
    ) {
        content()
    }
}

@Composable
private fun EditProfileCollapsingTopBar(
    name: String,
    bio: String,
    isLoading: Boolean,
    isSaving: Boolean,
    hasPendingMedia: Boolean,
    previewModel: Any?,
    shape: Shape,
    collapsedFraction: Float,
    onNavigateBack: () -> Unit,
    navigationEnabled: Boolean,
    onSave: () -> Unit,
    saveEnabled: Boolean,
) {
    val expandedProgress = (1f - collapsedFraction).coerceIn(0f, 1f)
    val appBarHeight = 56.dp + (104.dp * expandedProgress)
    val titleAlpha = ((collapsedFraction - 0.52f) / 0.48f).coerceIn(0f, 1f)
    val heroAlpha = ((1f - collapsedFraction) / 0.55f).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.84f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f)
                        )
                    )
                )
                .statusBarsPadding()
                .height(appBarHeight)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DPExtraLarge, vertical = DPMedium),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DPMedium)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    enabled = navigationEnabled
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBackIosNew,
                        contentDescription = stringResource(R.string.back),
                        modifier = backIconModifier()
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    modifier = Modifier.alpha(titleAlpha),
                    text = stringResource(R.string.edit_profile),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = onSave,
                    enabled = saveEnabled
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = stringResource(R.string.save)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = DPExtraLarge, vertical = DPMedium)
                    .graphicsLayer {
                        alpha = heroAlpha
                        translationY = 32f * collapsedFraction
                    }
            ) {
                EditProfileHeroCard(
                    name = name,
                    bio = bio,
                    isLoading = isLoading,
                    isSaving = isSaving,
                    hasPendingMedia = hasPendingMedia,
                    previewModel = previewModel,
                    shape = shape,
                    collapsedFraction = collapsedFraction
                )
            }
        }
    }
}

@Composable
private fun EditProfileHeroCard(
    name: String,
    bio: String,
    isLoading: Boolean,
    isSaving: Boolean,
    hasPendingMedia: Boolean,
    previewModel: Any?,
    shape: Shape,
    collapsedFraction: Float,
) {
    val expandedProgress = (1f - collapsedFraction).coerceIn(0f, 1f)
    val avatarSize = 48.dp + (28.dp * expandedProgress)
    val subtitleAlpha = expandedProgress
    val statusAlpha = ((expandedProgress - 0.2f) / 0.8f).coerceIn(0f, 1f)
    val pulseScaleBoost = 0.04f * expandedProgress
    val pulseAlphaBoost = 0.17f * expandedProgress

    val transition = rememberInfiniteTransition(label = "edit_profile_top_bar_hero")
    val haloScale by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.02f + pulseScaleBoost,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "edit_profile_top_bar_halo_scale"
    )
    val haloAlpha by transition.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.12f + pulseAlphaBoost,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "edit_profile_top_bar_halo_alpha"
    )

    val statusText = when {
        isSaving -> stringResource(R.string.edit_profile_status_saving)
        isLoading -> stringResource(R.string.edit_profile_status_loading)
        hasPendingMedia -> stringResource(R.string.edit_profile_status_pending)
        else -> stringResource(R.string.edit_profile_status_ready)
    }

    val headline = name.ifBlank { stringResource(R.string.profile) }
    val supportingText = bio.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.edit_profile_tagline)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DPSmall)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                modifier = Modifier
                    .size(avatarSize + 12.dp)
                    .graphicsLayer {
                        scaleX = haloScale
                        scaleY = haloScale
                    }
                    .alpha(haloAlpha),
                shape = shape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)
            ) {}
            ProfileMediaPreview(
                modifier = Modifier.size(avatarSize),
                previewModel = previewModel,
                isVideoPreview = false,
                shape = shape,
                emptyIcon = Icons.Rounded.Person
            )
        }

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = headline,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(subtitleAlpha),
            text = supportingText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Surface(
            modifier = Modifier.alpha(statusAlpha),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.52f)
        ) {
            Text(
                modifier = Modifier.padding(horizontal = DPLarge, vertical = 6.dp),
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ProfileMediaPickerCard(
    title: String,
    subtitle: String,
    previewModel: Any?,
    previewShape: Shape,
    isVideoPreview: Boolean,
    enabled: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onRemove: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.98f else 1f,
        animationSpec = spring(),
        label = "profile_media_picker_scale"
    )

    val iconContainerColor by animateColorAsState(
        targetValue = if (pressed && enabled) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.62f)
        },
        label = "profile_media_picker_icon_color"
    )

    PostContainer(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .alpha(if (enabled) 1f else 0.58f)
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (pressed) 5.dp else 9.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(DPMedium)
        ) {
            if (onRemove != null) {
                FilledIconButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp),
                    onClick = onRemove,
                    enabled = enabled,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = if (isVideoPreview) {
                            stringResource(R.string.remove_profile_video)
                        } else {
                            stringResource(R.string.remove_profile_image)
                        },
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(DPMedium)
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = iconContainerColor
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                ProfileMediaPreview(
                    modifier = Modifier.size(90.dp),
                    previewModel = previewModel,
                    isVideoPreview = isVideoPreview,
                    shape = previewShape
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(DPSmall)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileMediaPreview(
    previewModel: Any?,
    isVideoPreview: Boolean,
    shape: Shape,
    modifier: Modifier = Modifier,
    emptyIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.Add,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        if (previewModel == null) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = emptyIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Surface
        }

        Box {
            PlatformSubcomposeAsyncImage(
                model = rememberPlatformImageRequest(
                    data = previewModel,
                    options = PlatformImageRequestOptions(
                        crossfade = true,
                        videoFrameMillis = if (isVideoPreview) 0L else null,
                    )
                ),
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
                            imageVector = emptyIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            if (isVideoPreview) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                ) {
                    Icon(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(14.dp),
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
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
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        label = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(width = 26.dp, height = 18.dp),
                    shape = previewShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 0.9f else 0.5f)
                ) {}
                Text(text = label)
            }
        }
    )
}
