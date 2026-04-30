package com.dino.message.threadfeature.presentation.screen

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dino.message.corefeature.presentation.util.rememberLayoutDirection
import androidx.hilt.navigation.compose.hiltViewModel
import com.dino.message.chatfeature.presentation.component.MediaAccessLevel
import com.dino.message.chatfeature.presentation.component.MediaPermissionHandler
import com.dino.message.R
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.corefeature.presentation.util.backIconModifier
import com.dino.message.threadfeature.domain.model.PostMedia
import com.dino.message.threadfeature.presentation.component.Carousel
import com.dino.message.threadfeature.presentation.component.PostContainer
import com.dino.message.threadfeature.presentation.component.QuotedPostPreviewCard
import com.dino.message.threadfeature.presentation.viewmodel.CreatePostViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun CreatePostScreen(
    quotedPostId: Long = 0L
) {
    val viewModel: CreatePostViewModel = hiltViewModel()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val error by viewModel.error.collectAsState()
    val quotedPost by viewModel.quotedPost.collectAsState()
    val isQuotedPostLoading by viewModel.isQuotedPostLoading.collectAsState()
    val quotedPostError by viewModel.quotedPostError.collectAsState()
    val selectedMedia = viewModel.selectedMedia
    val context = LocalContext.current
    val previewMedia = selectedMedia.map {
        PostMedia(
            id = it.id,
            url = it.uri.toString(),
            mime_type = it.mimeType
        )
    }

    val canSubmit = viewModel.postDraft.value.trim().isNotEmpty()
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(
            CreatePostViewModel.MAX_MEDIA_FILES_PER_POST
        )
    ) { uris ->
        viewModel.onMediaPicked(uris)
    }
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val accessLevel = MediaPermissionHandler.resolveAccessLevel(permissions)
        if (accessLevel == MediaAccessLevel.DENIED) {
            Toast.makeText(
                context,
                context.getString(R.string.create_post_media_permission_required),
                Toast.LENGTH_SHORT
            ).show()
        } else {
            mediaPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
            )
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.create_post_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.closeScreen() },
                        enabled = !isSubmitting
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
                        onClick = { viewModel.submitPost() },
                        enabled = canSubmit && !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(R.string.post))
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




            Spacer(modifier = Modifier.height(DPSmall))
            Text(
                text = stringResource(R.string.preview),
                style = MaterialTheme.typography.titleMedium
            )

            PostContainer(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(DPMedium),
                    verticalArrangement = Arrangement.spacedBy(DPMedium)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(DPSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(30.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = null
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.you),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        Text(
                            text = stringResource(R.string.just_now),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    CompositionLocalProvider(LocalLayoutDirection provides rememberLayoutDirection(viewModel.postDraft.value)) {
                        OutlinedTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = viewModel.postDraft.value,
                            onValueChange = viewModel::onPostDraftChanged,
                            enabled = !isSubmitting,
                            placeholder = { Text(stringResource(R.string.create_post_placeholder)) },
                            minLines = 2,
                            maxLines = 10,
                            shape = MaterialTheme.shapes.extraLarge
                        )
                    }

                    AnimatedVisibility(visible = selectedMedia.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(DPSmall)) {
                            Carousel(
                                modifier = Modifier.fillMaxWidth(),
                                items = previewMedia
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(DPSmall),
                                contentPadding = PaddingValues(horizontal = DPSmall)
                            ) {
                                items(
                                    items = selectedMedia,
                                    key = { it.id }
                                ) { media ->
                                    AssistChip(
                                        onClick = { viewModel.removeSelectedMedia(media.id) },
                                        label = { Text(media.displayName, maxLines = 1) },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = stringResource(R.string.remove_media)
                                            )
                                        },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                        )
                                    )
                                }
                            }
                        }
                    }

                    if (viewModel.quotedPostId != null) {
                        Text(
                            text = stringResource(R.string.quoted_post),
                            style = MaterialTheme.typography.titleSmall
                        )
                        QuotedPostPreviewCard(
                            quotedPostId = viewModel.quotedPostId,
                            quotedPost = quotedPost
                        )
                        if (isQuotedPostLoading) {
                            Text(
                                text = stringResource(R.string.loading_quoted_post),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (quotedPostError != null) {
                            Text(
                                text = quotedPostError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.inversePrimary,
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = viewModel.followersOnly.value,
                                    enabled = !isSubmitting,
                                    onClick = {
                                        viewModel.onFollowersOnlyChanged(!viewModel.followersOnly.value)
                                    }
                                )
                                .padding(horizontal = DPMedium, vertical = DPSmall),
                            horizontalArrangement = Arrangement.spacedBy(DPSmall),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = viewModel.followersOnly.value,
                                onClick = { viewModel.onFollowersOnlyChanged(!viewModel.followersOnly.value) },
                                enabled = !isSubmitting
                            )
                            Column {
                                Text(
                                    text = stringResource(R.string.followers_only),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = stringResource(R.string.followers_only_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = error != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(
                                modifier = Modifier.padding(DPMedium),
                                text = stringResource(R.string.error_try_again, error.orEmpty()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }



            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = {
                        val currentAccess = MediaPermissionHandler.checkAccessLevel(context)
                        if (currentAccess != MediaAccessLevel.DENIED) {
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        } else {
                            mediaPermissionLauncher.launch(MediaPermissionHandler.getRequiredPermissions())
                        }
                    },
                    enabled = !isSubmitting && selectedMedia.size < CreatePostViewModel.MAX_MEDIA_FILES_PER_POST
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AttachFile,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(DPSmall))
                    Text(stringResource(R.string.add_media))
                }

                Text(
                    text = "${selectedMedia.size}/${CreatePostViewModel.MAX_MEDIA_FILES_PER_POST}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
