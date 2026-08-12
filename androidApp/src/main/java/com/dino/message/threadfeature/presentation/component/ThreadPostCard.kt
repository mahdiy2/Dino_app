package com.dino.message.threadfeature.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dino.message.R
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.corefeature.presentation.util.rememberLayoutDirection
import com.dino.message.corefeature.presentation.util.rememberTextAlign
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.graphics.Color
import com.dino.message.corefeature.presentation.util.getTimeAgo
import com.dino.message.corefeature.presentation.component.UserBadgeIcon
import com.dino.message.profilefeature.presentation.component.ProfileAvatarMedia
import com.dino.message.corefeature.presentation.util.UserBadgeRole
import com.dino.message.corefeature.presentation.util.parseUserBadgeRole
import com.dino.message.threadfeature.domain.model.Post
import com.dino.message.threadfeature.domain.model.PostMedia
import com.dino.message.threadfeature.presentation.util.artistTextOrUnknown
import com.dino.message.threadfeature.presentation.util.resolveMusicMetadata
import com.dino.message.corefeature.presentation.icons.AppIcons

@Composable
fun ThreadPostCard(
    modifier: Modifier = Modifier,
    post: Post,
    canDownloadMedia: Boolean = false,
    selectedPostVote: Long = 0L,
    postError: String? = null,
    showActions: Boolean = true,
    onUpVoteClick: (() -> Unit)? = null,
    onDownVoteClick: (() -> Unit)? = null,
    onCommentsClick: (() -> Unit)? = null,
    onQuoteClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    onQuotedPostClick: ((Long) -> Unit)? = null,
    onUserClick: (() -> Unit)? = null,
    onReportClick: (() -> Unit)? = null,
    onBlockUserClick: (() -> Unit)? = null,
    onEditPostClick: (() -> Unit)? = null,
    onDeletePostClick: (() -> Unit)? = null,
    onAddTrackToPlaylistClick: ((PostMedia) -> Unit)? = null,
    onShowTrackPlaylistAddersClick: ((PostMedia, String) -> Unit)? = null,
    onPostLongClick: ((Long) -> Unit)? = null,
    showHistoricalWinnerMarkOnProfile: Boolean = false
) {
    val commentsContentColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "comment"
    )
    var showPostMenu by remember { mutableStateOf(false) }
    val isCurrentWinner = post.isDailyWinner
    val shouldShowHistoricalWinner = showHistoricalWinnerMarkOnProfile && !isCurrentWinner && post.wasDailyWinner
    val winnerAccent = winnerAccentColorForBadge(post.author.badge)
    val winnerBorderColor = winnerAccent.copy(alpha = if (isCurrentWinner) 0.65f else 0.42f)
    val winnerLabelContainerColor = winnerAccent.copy(alpha = 0.16f)
    val winnerLabelContentColor = winnerAccent.copy(alpha = 0.95f)
    val winnerLabelText = when {
        isCurrentWinner -> stringResource(R.string.post_winner_today_label)
        shouldShowHistoricalWinner -> stringResource(R.string.post_winner_history_label)
        else -> null
    }

    val postContainerModifier = if (onPostLongClick != null) {
        modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .combinedClickable(
                onClick = {},
                onLongClick = { onPostLongClick(post.id) }
            )
    } else {
        modifier
    }

    PostContainer(
        modifier = postContainerModifier,
        borderColor = if (isCurrentWinner) winnerBorderColor else null
    ) {
        Column(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
                .padding(DPMedium)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DPSmall),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = if (onUserClick != null) {
                        Modifier
                            .clip(MaterialTheme.shapes.extraLarge)
                            .clickable { onUserClick() }
                    } else {
                        Modifier
                            .clip(MaterialTheme.shapes.extraLarge)
                    }
                ) {
                    PostAuthorAvatar(
                        imageUrl = post.author.profile_image_url,
                        badge = post.author.badge,
                        profileImageShape = post.author.profile_image_shape,
                        modifier = Modifier.size(46.dp)
                    )
                    Column(
                        modifier = Modifier.padding(end = DPMedium)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = post.author.name?.takeIf { it.isNotBlank() }
                                    ?: post.author.username,
                                style = MaterialTheme.typography.labelLarge
                            )
                            UserBadgeIcon(
                                badge = post.author.badge,
                                size = 16.dp
                            )
                        }
                        if (!post.author.name.isNullOrBlank()) {
                            Text(
                                text = "@${post.author.username}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (winnerLabelText != null) {
                        AssistChip(
                            onClick = {},
                            enabled = false,
                            label = {
                                Text(
                                    text = winnerLabelText,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = winnerLabelContainerColor,
                                labelColor = winnerLabelContentColor,
                                disabledContainerColor = winnerLabelContainerColor,
                                disabledLabelColor = winnerLabelContentColor
                            ),
                            border = null
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(R.string.time_ago_format, getTimeAgo(post.created_at)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (post.isEdited) {
                            Text(
                                text = stringResource(R.string.post_edited_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (
                        onReportClick != null ||
                        onBlockUserClick != null ||
                        onEditPostClick != null ||
                        onDeletePostClick != null
                    ) {
                        Box {
                            IconButton(
                                onClick = { showPostMenu = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = AppIcons.MoreVert,
                                    contentDescription = stringResource(R.string.post_actions),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = showPostMenu,
                                onDismissRequest = { showPostMenu = false }
                            ) {
                                if (onReportClick != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.report)) },
                                        onClick = {
                                            showPostMenu = false
                                            onReportClick()
                                        }
                                    )
                                }
                                if (onBlockUserClick != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.block_user)) },
                                        onClick = {
                                            showPostMenu = false
                                            onBlockUserClick()
                                        }
                                    )
                                }
                                if (onEditPostClick != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.edit_post_action)) },
                                        onClick = {
                                            showPostMenu = false
                                            onEditPostClick()
                                        }
                                    )
                                }
                                if (onDeletePostClick != null) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.delete)) },
                                        onClick = {
                                            showPostMenu = false
                                            onDeletePostClick()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }


            Spacer(modifier = Modifier.height(DPSmall))
            if (post.text.isNotBlank()) {
                CompositionLocalProvider(LocalLayoutDirection provides rememberLayoutDirection(post.text)) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = post.text,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = rememberTextAlign(post.text)
                    )
                }
            }

            val musicMedia = post.media.firstOrNull { it.mime_type.startsWith("audio/", ignoreCase = true) }
            if (musicMedia != null) {
                val resolvedMusicMetadata = resolveMusicMetadata(
                    displayName = musicMedia.display_name,
                    title = musicMedia.title,
                    artist = musicMedia.artist
                )
                MusicTrackPlayerCard(
                    modifier = Modifier.padding(top = DPMedium),
                    trackUrl = musicMedia.url,
                    trackTitle = resolvedMusicMetadata.title,
                    trackSubtitle = artistTextOrUnknown(resolvedMusicMetadata.artist),
                    playlistAdders = musicMedia.playlistAdders,
                    onPlaylistAddersClick = if (onShowTrackPlaylistAddersClick != null) {
                        {
                            onShowTrackPlaylistAddersClick(
                                musicMedia,
                                resolvedMusicMetadata.title
                            )
                        }
                    } else {
                        null
                    },
                    onAddToPlaylistClick = if (onAddTrackToPlaylistClick != null) {
                        { onAddTrackToPlaylistClick(musicMedia) }
                    } else {
                        null
                    }
                )
            }

            val visualMedia = post.media.filterNot { it.mime_type.startsWith("audio/", ignoreCase = true) }
            if (visualMedia.isNotEmpty()) {
                Carousel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = DPMedium),
                    items = visualMedia,
                    canDownloadMedia = canDownloadMedia
                )
            }

            if (post.quotedPostId != null || post.quotedPost != null) {
                QuotedPostPreviewCard(
                    modifier = Modifier.padding(top = DPMedium),
                    quotedPostId = post.quotedPostId,
                    quotedPost = post.quotedPost,
                    onClick = onQuotedPostClick
                )
            }

            if (showActions) {
                Spacer(modifier = Modifier.height(DPMedium))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
                            shape = MaterialTheme.shapes.extraLarge
                        )
                        .padding(DPMedium),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(DPSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VoteIconButton(
                            selected = selectedPostVote == 1L,
                            onClick = { onUpVoteClick?.invoke() },
                            icon = R.drawable.ic_like_filled
                        )

                        VoteIconButton(
                            selected = selectedPostVote == (-1L),
                            onClick = { onDownVoteClick?.invoke() },
                            icon = R.drawable.ic_dislike_filled
                        )

                        IconButton(
                            onClick = { onCommentsClick?.invoke() },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = commentsContentColor
                            )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_commets_filled),
                                contentDescription = stringResource(R.string.comments)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onQuoteClick?.invoke() },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = commentsContentColor
                            )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_quote_post_filled),
                                contentDescription = stringResource(R.string.quote_post)
                            )
                        }
                        IconButton(
                            onClick = { onShareClick?.invoke() },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = commentsContentColor
                            )
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_send_filled),
                                contentDescription = stringResource(R.string.share_post)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = postError != null,
                enter = fadeIn() + expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
                exit = fadeOut() + shrinkVertically()
            ) {
                postError?.let {
                    Spacer(modifier = Modifier.height(DPSmall))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            modifier = Modifier.padding(DPSmall),
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PostAuthorAvatar(
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

private fun winnerAccentColorForBadge(badge: String?): Color {
    return when (parseUserBadgeRole(badge)) {
        UserBadgeRole.STAFF -> Color(0xFF111111)
        UserBadgeRole.MODERATOR -> Color(0xFF1E8E3E)
        UserBadgeRole.VERIFIED -> Color(0xFF2563EB)
        UserBadgeRole.POST_OF_THE_DAY -> Color(0xFFE07A00)
        null -> Color(0xFFB45309)
    }
}
