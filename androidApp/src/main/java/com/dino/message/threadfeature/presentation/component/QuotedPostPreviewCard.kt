package com.dino.message.threadfeature.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dino.message.R
import com.dino.message.corefeature.presentation.component.UserBadgeIcon
import com.dino.message.corefeature.presentation.platform.PlatformAsyncImage
import com.dino.message.corefeature.presentation.platform.PlatformImageRequestOptions
import com.dino.message.corefeature.presentation.platform.rememberPlatformImageRequest
import com.dino.message.threadfeature.domain.model.QuotedPostPreview

@Composable
fun QuotedPostPreviewCard(
    quotedPostId: Long?,
    quotedPost: QuotedPostPreview?,
    modifier: Modifier = Modifier,
    onClick: ((Long) -> Unit)? = null,
) {
    val targetPostId = quotedPost?.id ?: quotedPostId
    val canOpen = targetPostId != null && onClick != null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (canOpen) {
                    Modifier
                        .clip(MaterialTheme.shapes.extraLarge)
                        .clickable { onClick?.invoke(targetPostId!!) }
                } else {
                    Modifier
                        .clip(MaterialTheme.shapes.extraLarge)
                }
            )
            .background(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.extraLarge
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.extraLarge
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_quote_post_filled),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            if (quotedPost != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.quoted_post_from, quotedPost.author.username),
                        style = MaterialTheme.typography.labelLarge
                    )
                    UserBadgeIcon(
                        badge = quotedPost.author.badge,
                        size = 16.dp
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.quoted_post_unavailable),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        if (quotedPost != null) {
            val visualMedia = quotedPost.media.filterNot {
                it.mime_type.startsWith("audio/", ignoreCase = true)
            }

            if (visualMedia.isNotEmpty()) {
                CompactQuotedVisualMediaRow(
                    urls = visualMedia.map { it.url },
                    isVideo = visualMedia.map { it.mime_type.startsWith("video/", ignoreCase = true) }
                )
            }

            if (quotedPost.text.isNotBlank()) {
                Text(
                    text = quotedPost.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4
                )
            }

            val hasMusic = quotedPost.media.any { it.mime_type.startsWith("audio/", ignoreCase = true) }
            val visualCount = quotedPost.media.count {
                !it.mime_type.startsWith("audio/", ignoreCase = true)
            }

            if (hasMusic) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_music_note_filled),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = stringResource(R.string.quoted_post_includes_music),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (visualCount > 0) {
                Text(
                    text = stringResource(R.string.quoted_post_media_count, visualCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (canOpen) {
            Text(
                text = stringResource(R.string.quoted_post_open_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CompactQuotedVisualMediaRow(
    urls: List<String>,
    isVideo: List<Boolean>,
) {
    val context = LocalContext.current
    val visibleItems = urls.take(3)
    val remainingCount = urls.size - visibleItems.size

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        visibleItems.forEachIndexed { index, url ->
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            ) {
                PlatformAsyncImage(
                    model = rememberPlatformImageRequest(
                        data = url,
                        options = PlatformImageRequestOptions(crossfade = true)
                    ),
                    contentDescription = stringResource(R.string.quoted_post_media_preview),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                if (isVideo.getOrNull(index) == true) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(2.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.62f)
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            text = stringResource(R.string.video),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        if (remainingCount > 0) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    text = "+$remainingCount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
