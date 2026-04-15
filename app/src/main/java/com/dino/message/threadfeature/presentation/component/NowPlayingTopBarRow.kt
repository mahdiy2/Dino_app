package com.dino.message.threadfeature.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dino.message.threadfeature.presentation.music.MusicPlaybackManager

@Composable
fun NowPlayingTopBarRow(
    onOpenPlaylist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playbackState by MusicPlaybackManager.playbackState.collectAsState()
    val currentTrackUrl = playbackState.currentTrackUrl

    AnimatedVisibility(
        visible = !currentTrackUrl.isNullOrBlank(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        val rowShape = RoundedCornerShape(28.dp)
        val coolGradient = Brush.horizontalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.tertiaryContainer
            )
        )
        val contentColor = MaterialTheme.colorScheme.onPrimaryContainer

        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = rowShape,
            color = Color.Transparent,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = coolGradient, shape = rowShape)
                    .clickable(onClick = { onOpenPlaylist(currentTrackUrl.orEmpty()) }),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = {
                        MusicPlaybackManager.toggleTrack(
                            context = context,
                            trackUrl = currentTrackUrl.orEmpty(),
                            title = playbackState.title,
                            artist = playbackState.artist
                        )
                    }
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) {
                            Icons.Rounded.Pause
                        } else {
                            Icons.Rounded.PlayArrow
                        },
                        contentDescription = if (playbackState.isPlaying) "Pause music" else "Play music",
                        tint = contentColor
                    )
                }

                Text(
                    modifier = Modifier.weight(1f),
                    text = playbackState.title ?: "Music track",
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                IconButton(
                    onClick = {
                        MusicPlaybackManager.stop(context)
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close music player",
                        modifier = Modifier.size(20.dp),
                        tint = contentColor
                    )
                }
            }
        }
    }
}
