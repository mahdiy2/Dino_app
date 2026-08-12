package com.dino.message.threadfeature.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.dino.message.R
import com.dino.message.corefeature.presentation.component.UserBadgeIcon
import com.dino.message.profilefeature.presentation.component.ProfileAvatarMedia
import com.dino.message.threadfeature.domain.model.Author
import com.dino.message.threadfeature.presentation.viewmodel.TrackPlaylistAddersDialogViewModel
import kotlinx.serialization.json.Json

@Composable
fun TrackPlaylistAddersDialogScreen(
    trackTitle: String,
    playlistAddersJson: String,
) {
    val viewModel: TrackPlaylistAddersDialogViewModel = koinViewModel()
    val playlistAdders = remember(playlistAddersJson) {
        runCatching {
            Json.decodeFromString<List<Author>>(playlistAddersJson)
        }.getOrDefault(emptyList())
    }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.track_adders_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = trackTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (playlistAdders.isEmpty()) {
                Text(
                    text = stringResource(R.string.track_adders_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = playlistAdders,
                        key = { it.id }
                    ) { user ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlaylistAdderAvatar(
                                imageUrl = user.profile_image_url,
                                badge = user.badge,
                                profileImageShape = user.profile_image_shape,
                                modifier = Modifier.size(36.dp)
                            )
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = user.name?.takeIf { it.isNotBlank() } ?: user.username,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    UserBadgeIcon(
                                        badge = user.badge,
                                        size = 16.dp
                                    )
                                }
                                Text(
                                    text = "@${user.username}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = viewModel::dismissDialog) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    }
}

@Composable
private fun PlaylistAdderAvatar(
    imageUrl: String?,
    badge: String?,
    profileImageShape: String?,
    modifier: Modifier = Modifier
) {
    ProfileAvatarMedia(
        modifier = modifier,
        profileImageUrl = imageUrl,
        profileVideoUrl = null,
        badge = badge,
        profileImageShape = profileImageShape
    )
}
