package com.dino.message.threadfeature.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dino.message.R
import com.dino.message.corefeature.presentation.component.UserBadgeIcon
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.corefeature.presentation.util.getRelativeTimeLabel
import com.dino.message.corefeature.presentation.util.rememberLayoutDirection
import com.dino.message.corefeature.presentation.util.rememberTextAlign
import com.dino.message.profilefeature.presentation.component.ProfileAvatarMedia
import com.dino.message.threadfeature.domain.model.Comment
import com.dino.message.threadfeature.presentation.viewmodel.ThreadCommentsViewModel
import com.dino.message.corefeature.presentation.icons.AppIcons

enum class ThreadCommentsSectionStyle {
    Embedded,
    BottomSheet
}

@Composable
fun ThreadCommentsSection(
    viewModel: ThreadCommentsViewModel,
    modifier: Modifier = Modifier,
    style: ThreadCommentsSectionStyle = ThreadCommentsSectionStyle.Embedded,
    bottomSheetListMaxHeight: Dp = 560.dp
) {
    val isLoadingComments by viewModel.isCommentsLoading.collectAsState()
    val hasMoreComments by viewModel.hasMoreComments.collectAsState()
    val commentsError by viewModel.error.collectAsState()
    val isSendingComment by viewModel.isCommentSending.collectAsState()
    val expandedNestedRepliesByComment = remember(viewModel.postId) { mutableStateMapOf<Long, Boolean>() }

    val content: @Composable () -> Unit = {
        Column(
            verticalArrangement = Arrangement.spacedBy(DPSmall)
        ) {
            CommentsHeader()
            CommentComposer(
                value = viewModel.commentDraft.value,
                enabled = !isSendingComment,
                onValueChange = { viewModel.commentDraft.value = it },
                onSendClick = { viewModel.submitComment() }
            )

            commentsError?.let { message ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        modifier = Modifier.padding(DPSmall),
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (style == ThreadCommentsSectionStyle.BottomSheet) {
                CommentsLazyContent(
                    viewModel = viewModel,
                    isLoadingComments = isLoadingComments,
                    hasMoreComments = hasMoreComments,
                    expandedNestedRepliesByComment = expandedNestedRepliesByComment,
                    maxHeight = bottomSheetListMaxHeight
                )
            } else {
                CommentsInlineContent(
                    viewModel = viewModel,
                    isLoadingComments = isLoadingComments,
                    hasMoreComments = hasMoreComments,
                    expandedNestedRepliesByComment = expandedNestedRepliesByComment
                )
            }
        }
    }

    if (style == ThreadCommentsSectionStyle.Embedded) {
        Surface(
            modifier = modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    shape = MaterialTheme.shapes.extraLarge
                ),
            color = Color.Transparent,
            tonalElevation = 0.dp,
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Box(modifier = Modifier.padding(DPMedium)) {
                content()
            }
        }
    } else {
        Box(modifier = modifier) {
            content()
        }
    }
}

@Composable
private fun CommentsHeader() {
    Text(
        text = stringResource(R.string.comments),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun CommentsInlineContent(
    viewModel: ThreadCommentsViewModel,
    isLoadingComments: Boolean,
    hasMoreComments: Boolean,
    expandedNestedRepliesByComment: MutableMap<Long, Boolean>
) {
    when {
        isLoadingComments && viewModel.comments.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = DPMedium),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        !isLoadingComments && viewModel.comments.isEmpty() -> {
            EmptyCommentsState()
        }

        else -> {
            CommentTree(
                comments = viewModel.comments,
                depth = 0,
                viewModel = viewModel,
                expandedNestedRepliesByComment = expandedNestedRepliesByComment
            )

            if (isLoadingComments) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = DPMedium),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            if (hasMoreComments) {
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.loadMoreComments() },
                    enabled = !isLoadingComments
                ) {
                    Text(
                        text = if (isLoadingComments) {
                            stringResource(R.string.comments_loading)
                        } else {
                            stringResource(R.string.comments_load_more)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentsLazyContent(
    viewModel: ThreadCommentsViewModel,
    isLoadingComments: Boolean,
    hasMoreComments: Boolean,
    expandedNestedRepliesByComment: MutableMap<Long, Boolean>,
    maxHeight: Dp
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight),
        verticalArrangement = Arrangement.spacedBy(DPSmall)
    ) {
        if (!isLoadingComments && viewModel.comments.isEmpty()) {
            item {
                EmptyCommentsState()
            }
        } else {
            if (isLoadingComments && viewModel.comments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(DPMedium),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                item {
                    CommentTree(
                        comments = viewModel.comments,
                        depth = 0,
                        viewModel = viewModel,
                        expandedNestedRepliesByComment = expandedNestedRepliesByComment
                    )
                }
            }

            if (isLoadingComments && viewModel.comments.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(DPMedium),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (hasMoreComments) {
                item {
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.loadMoreComments() },
                        enabled = !isLoadingComments
                    ) {
                        Text(
                            text = if (isLoadingComments) {
                                stringResource(R.string.comments_loading)
                            } else {
                                stringResource(R.string.comments_load_more)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCommentsState() {
    Image(
        painter = painterResource(R.drawable.empty_comments),
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = DPMedium, start = DPMedium, end = DPMedium)
    )

    Text(
        text = stringResource(R.string.comments_empty_title),
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleMedium.copy(
            textAlign = TextAlign.Center
        ),
        color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = stringResource(R.string.comments_empty_subtitle),
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium.copy(
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(DPExtraLarge))
}

@Composable
private fun CommentTree(
    comments: List<Comment>,
    depth: Int,
    viewModel: ThreadCommentsViewModel,
    expandedNestedRepliesByComment: MutableMap<Long, Boolean>
) {
    val visibleComments = if (depth == 0) {
        comments.filter { it.parent_id == null }
    } else {
        comments
    }

    visibleComments.forEachIndexed { index, comment ->
        val hasReplies = comment.replies.isNotEmpty()
        val areRepliesVisible = if (hasReplies) {
            expandedNestedRepliesByComment[comment.id] == true
        } else {
            true
        }

        CommentItem(
            comment = comment,
            depth = depth,
            viewModel = viewModel,
            shouldShowRepliesToggle = hasReplies,
            areRepliesVisible = areRepliesVisible,
            repliesCount = comment.replies.size,
            onAuthorClick = { viewModel.openUserProfile(comment.author.username) },
            onToggleReplies = {
                expandedNestedRepliesByComment[comment.id] = !areRepliesVisible
            }
        )

        if (index != visibleComments.lastIndex) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 2.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        if (hasReplies && areRepliesVisible) {
            CommentTree(
                comments = comment.replies,
                depth = depth + 1,
                viewModel = viewModel,
                expandedNestedRepliesByComment = expandedNestedRepliesByComment
            )
        }
    }
}

@Composable
private fun CommentItem(
    comment: Comment,
    depth: Int,
    viewModel: ThreadCommentsViewModel,
    shouldShowRepliesToggle: Boolean,
    areRepliesVisible: Boolean,
    repliesCount: Int,
    onAuthorClick: () -> Unit,
    onToggleReplies: () -> Unit
) {
    val selectedVote = viewModel.selectedCommentVote(comment.id)
    val isReplyVisible = viewModel.isReplyComposerVisible(comment.id)
    val isSendingComment by viewModel.isCommentSending.collectAsState()
    val isDeletingComment by viewModel.isCommentDeleting.collectAsState()
    val guideColor = when (depth % 3) {
        0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
        1 -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.30f)
        else -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.30f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 12).dp, top = 4.dp)
            .drawBehind {
                if (depth > 0) {
                    drawRect(
                        color = guideColor,
                        topLeft = Offset(0f, 0f),
                        size = Size(3.dp.toPx(), size.height)
                    )
                }
            }
            .padding(start = if (depth > 0) 10.dp else 0.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
    ) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier.padding(DPMedium),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onAuthorClick),
                    horizontalArrangement = Arrangement.spacedBy(DPSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CommentAuthorAvatar(
                        imageUrl = comment.author.profile_image_url,
                        badge = comment.author.badge,
                        profileImageShape = comment.author.profile_image_shape,
                        modifier = Modifier.size(34.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = comment.author.name?.takeIf { it.isNotBlank() }
                                    ?: comment.author.username,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            UserBadgeIcon(
                                badge = comment.author.badge,
                                size = 15.dp
                            )
                        }
                        Text(
                            text = "@${comment.author.username}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = getRelativeTimeLabel(comment.created_at),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = stringResource(R.string.comments_score_format, comment.score),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                CompositionLocalProvider(LocalLayoutDirection provides rememberLayoutDirection(comment.text)) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = comment.text,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = rememberTextAlign(comment.text),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(DPSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VoteIconButton(
                        selected = selectedVote == 1L,
                        onClick = { viewModel.voteComment(commentId = comment.id, value = 1) },
                        icon = R.drawable.ic_like_filled
                    )
                    VoteIconButton(
                        selected = selectedVote == -1L,
                        onClick = { viewModel.voteComment(commentId = comment.id, value = -1) },
                        icon = R.drawable.ic_dislike_filled
                    )
                    TextButton(onClick = { viewModel.toggleReplyComposer(comment.id) }) {
                        Icon(
                            imageVector = AppIcons.Reply,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = if (isReplyVisible) {
                                stringResource(R.string.cancel)
                            } else {
                                stringResource(R.string.reply)
                            }
                        )
                    }
                    if (viewModel.canDeleteComment(comment)) {
                        TextButton(
                            onClick = { viewModel.deleteOwnComment(comment.id) },
                            enabled = !isDeletingComment
                        ) {
                            Text(
                                text = if (isDeletingComment) {
                                    stringResource(R.string.deleting)
                                } else {
                                    stringResource(R.string.delete)
                                },
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = shouldShowRepliesToggle) {
                    TextButton(onClick = onToggleReplies) {
                        Text(
                            text = if (areRepliesVisible) {
                                stringResource(R.string.comments_hide_replies)
                            } else {
                                stringResource(R.string.comments_show_more, repliesCount)
                            }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isReplyVisible,
            enter = fadeIn() + expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
            exit = fadeOut() + shrinkVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        ) {
            Column(modifier = Modifier.padding(top = DPSmall)) {
                CommentComposer(
                    value = viewModel.replyDraft(comment.id),
                    enabled = !isSendingComment,
                    onValueChange = { viewModel.onReplyDraftChanged(comment.id, it) },
                    onSendClick = { viewModel.submitComment(parentId = comment.id) }
                )
            }
        }
    }
}

@Composable
private fun CommentAuthorAvatar(
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

@Composable
private fun CommentComposer(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DPSmall, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides rememberLayoutDirection(value)) {
                TextField(
                    modifier = Modifier.weight(1f),
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    maxLines = 4,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        textAlign = rememberTextAlign(value)
                    ),
                    placeholder = {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = stringResource(R.string.comments_write_placeholder),
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = rememberTextAlign(value)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent
                    )
                )
            }
            Spacer(modifier = Modifier.width(DPSmall))
            IconButton(
                onClick = onSendClick,
                enabled = enabled && value.isNotBlank()
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_send_filled),
                    contentDescription = stringResource(R.string.send_comment),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
