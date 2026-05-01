package com.dino.message.threadfeature.presentation.screen

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Person
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.dino.message.R
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.corefeature.presentation.util.backIconModifier
import com.dino.message.corefeature.presentation.util.rememberLayoutDirection
import com.dino.message.threadfeature.presentation.component.MusicTrackPlayerCard
import com.dino.message.threadfeature.presentation.component.PostContainer
import com.dino.message.threadfeature.presentation.component.QuotedPostPreviewCard
import com.dino.message.threadfeature.presentation.util.artistTextOrUnknown
import com.dino.message.threadfeature.presentation.viewmodel.PostMusicViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun PostMusicScreen(
    quotedPostId: Long = 0L
) {
    val viewModel: PostMusicViewModel = hiltViewModel()
    val context = LocalContext.current
    val selectedTrack = viewModel.selectedTrack.value
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val error by viewModel.error.collectAsState()
    val quotedPost by viewModel.quotedPost.collectAsState()
    val isQuotedPostLoading by viewModel.isQuotedPostLoading.collectAsState()
    val quotedPostError by viewModel.quotedPostError.collectAsState()
    val canSubmit = selectedTrack != null

    val musicPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {
        }
        viewModel.onMusicPicked(uri)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.post_music_title)) },
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
                            placeholder = { Text(stringResource(R.string.post_music_caption_placeholder)) },
                            minLines = 2,
                            maxLines = 10,
                            shape = MaterialTheme.shapes.extraLarge
                        )
                    }

                    if (selectedTrack != null) {
                        MusicTrackPlayerCard(
                            trackUrl = selectedTrack.uri.toString(),
                            trackTitle = selectedTrack.title,
                            trackSubtitle = artistTextOrUnknown(selectedTrack.artist),
                            enabled = !isSubmitting
                        )
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Text(
                                modifier = Modifier.padding(DPMedium),
                                text = stringResource(R.string.post_music_choose_track),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                        musicPickerLauncher.launch(arrayOf("*/*"))
                    },
                    enabled = !isSubmitting
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LibraryMusic,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(DPSmall))
                    Text(
                        if (selectedTrack == null) {
                            stringResource(R.string.post_music_select)
                        } else {
                            stringResource(R.string.post_music_replace)
                        }
                    )
                }

                if (selectedTrack != null) {
                    IconButton(
                        onClick = { viewModel.clearSelectedTrack() },
                        enabled = !isSubmitting
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.post_music_remove_selected)
                        )
                    }
                }
            }

            Text(
                text = if (selectedTrack == null) {
                    stringResource(R.string.post_music_required)
                } else {
                    stringResource(R.string.post_music_selected_format, selectedTrack.displayName)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(DPSmall))
        }
    }
}
