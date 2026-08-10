package com.dino.message.activitynotificationfeature.presentation.component

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dino.message.R
import com.dino.message.activitynotificationfeature.domain.model.ActivityNotification
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPLarge
import com.dino.message.corefeature.presentation.util.getRelativeTimeLabel
import com.dino.message.profilefeature.presentation.component.ProfileAvatarMedia
import com.dino.message.corefeature.presentation.icons.AppIcons

@Composable
fun ActivityNotificationItem(
    notification: ActivityNotification,
    showFollowBackButton: Boolean,
    isFollowBackLoading: Boolean,
    onOpenProfile: (String) -> Unit,
    onFollowBackClick: () -> Unit,
    showReplyButton: Boolean,
    isReplyComposerVisible: Boolean,
    replyDraft: String,
    isReplySending: Boolean,
    onNotificationClick: () -> Unit,
    onToggleReplyComposer: () -> Unit,
    onReplyDraftChanged: (String) -> Unit,
    onSendReply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val bgColor = if (!notification.is_read) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onNotificationClick)
            .padding(horizontal = DPExtraLarge, vertical = DPLarge),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DPLarge)
    ) {
        ProfileAvatarMedia(
            modifier = Modifier
                .size(44.dp)
                .clickable { onOpenProfile(notification.actor.username) },
            profileImageUrl = notification.actor.profile_image_url,
            badge = notification.actor.badge,
            profileImageShape = notification.actor.profile_image_shape,
            profileVideoUrl = null
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = buildNotificationTitle(notification, context),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (!notification.is_read) FontWeight.SemiBold else FontWeight.Normal
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            val detailLines = buildNotificationDetails(notification, context)
            if (detailLines.isNotEmpty()) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    NotificationDetailSection(detailLines)
                }
            } else {
                val subtitle = buildNotificationSubtitle(notification, context)
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = getRelativeTimeLabel(notification.created_at),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )

            if (showFollowBackButton) {
                FilledTonalButton(
                    onClick = onFollowBackClick,
                    enabled = !isFollowBackLoading
                ) {
                    if (isFollowBackLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = stringResource(R.string.follow_back))
                    }
                }
            }

            if (showReplyButton) {
                TextButton(onClick = onToggleReplyComposer) {
                    Icon(
                        imageVector = AppIcons.Reply,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = if (isReplyComposerVisible) {
                            stringResource(R.string.cancel)
                        } else {
                            stringResource(R.string.reply)
                        }
                    )
                }
            }

            if (showReplyButton && isReplyComposerVisible) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        modifier = Modifier.weight(1f),
                        value = replyDraft,
                        onValueChange = onReplyDraftChanged,
                        enabled = !isReplySending,
                        maxLines = 4,
                        placeholder = {
                            Text(
                                stringResource(R.string.notification_write_reply),
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent
                        )
                    )
                    IconButton(
                        onClick = onSendReply,
                        enabled = !isReplySending && replyDraft.isNotBlank()
                    ) {
                        if (isReplySending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_send_filled),
                                contentDescription = stringResource(R.string.send_reply),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationDetailSection(lines: List<NotificationDetailLine>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .padding(top = 4.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        lines.forEachIndexed { index, line ->
            val rowVisibility = remember(index, line.label, line.value) {
                MutableTransitionState(false).apply { targetState = true }
            }
            AnimatedVisibility(
                visibleState = rowVisibility,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 180,
                        delayMillis = index * 45
                    )
                ) + expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = line.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = line.value,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

private fun buildNotificationTitle(notification: ActivityNotification, context: Context): String {
    val actorName = notification.actor.name ?: notification.actor.username
    return when (notification.kind) {
        "follow" -> context.getString(R.string.notification_title_follow, actorName)
        "unfollow" -> context.getString(R.string.notification_title_unfollow, actorName)
        "comment" -> {
            val postPreview = quotedPreview(notification.extra?.post_text_preview, 40)
                ?: context.getString(R.string.notification_your_post)
            context.getString(R.string.notification_title_comment, actorName, postPreview)
        }

        "comment_reply" -> {
            val postPreview = quotedPreview(notification.extra?.post_text_preview, 40)
                ?: context.getString(R.string.notification_your_post)
            context.getString(R.string.notification_title_comment_reply, actorName, postPreview)
        }

        "vote" -> {
            val label = notification.extra?.vote_label ?: context.getString(R.string.notification_vote_label_default)
            val postPreview = quotedPreview(notification.extra?.post_text_preview, 40)
                ?: context.getString(R.string.notification_your_post)
            context.getString(R.string.notification_title_vote, actorName, label, postPreview)
        }

        "post_like_milestone" -> {
            val count = notification.extra?.engagement_count ?: notification.extra?.milestone_count
            val postPreview = quotedPreview(notification.extra?.post_text_preview, 40)
                ?: context.getString(R.string.notification_your_post)
            if (count != null) {
                context.getString(R.string.notification_title_like_milestone_count, postPreview, count)
            } else {
                context.getString(R.string.notification_title_like_milestone_many, postPreview)
            }
        }

        "post_comment_milestone" -> {
            val count = notification.extra?.engagement_count ?: notification.extra?.milestone_count
            val postPreview = quotedPreview(notification.extra?.post_text_preview, 40)
                ?: context.getString(R.string.notification_your_post)
            if (count != null) {
                context.getString(R.string.notification_title_comment_milestone_count, postPreview, count)
            } else {
                context.getString(R.string.notification_title_comment_milestone_many, postPreview)
            }
        }

        "story_mention" -> context.getString(R.string.notification_title_story_mention, actorName)
        "story_reply" -> context.getString(R.string.notification_title_story_reply, actorName)
        else -> context.getString(R.string.notification_title_interacted, actorName)
    }
}

private data class NotificationDetailLine(
    val label: String,
    val value: String
)

private fun buildNotificationDetails(
    notification: ActivityNotification,
    context: Context
): List<NotificationDetailLine> {
    return when (notification.kind) {
        "comment" -> {
            val commentPreview = notification.extra?.comment_preview?.trim().orEmpty()
            val postPreview = notification.extra?.post_text_preview?.trim().orEmpty()
            buildList {
                if (commentPreview.isNotBlank()) {
                    add(
                        NotificationDetailLine(
                            label = context.getString(R.string.notification_detail_label_comment),
                            value = truncateText(commentPreview, 100)
                        )
                    )
                }
                if (postPreview.isNotBlank()) {
                    add(
                        NotificationDetailLine(
                            label = context.getString(R.string.notification_detail_label_post),
                            value = truncateText(postPreview, 100)
                        )
                    )
                }
            }
        }

        "comment_reply" -> {
            val replyPreview = notification.extra?.comment_preview?.trim().orEmpty()
            val parentPreview = notification.extra?.parent_comment_preview?.trim().orEmpty()
            val postPreview = notification.extra?.post_text_preview?.trim().orEmpty()
            buildList {
                if (replyPreview.isNotBlank()) {
                    add(
                        NotificationDetailLine(
                            label = context.getString(R.string.notification_detail_label_reply),
                            value = truncateText(replyPreview, 100)
                        )
                    )
                }
                if (parentPreview.isNotBlank()) {
                    add(
                        NotificationDetailLine(
                            label = context.getString(R.string.notification_detail_label_on_comment),
                            value = truncateText(parentPreview, 100)
                        )
                    )
                }
                if (postPreview.isNotBlank()) {
                    add(
                        NotificationDetailLine(
                            label = context.getString(R.string.notification_detail_label_post),
                            value = truncateText(postPreview, 100)
                        )
                    )
                }
            }
        }

        "story_reply" -> {
            val preview = notification.extra?.reply_preview?.trim().orEmpty()
            if (preview.isBlank()) emptyList()
            else {
                listOf(
                    NotificationDetailLine(
                        label = context.getString(R.string.notification_detail_label_reply),
                        value = truncateText(preview, 100)
                    )
                )
            }
        }

        else -> emptyList()
    }
}

private fun buildNotificationSubtitle(notification: ActivityNotification, context: Context): String? {
    return when (notification.kind) {
        "comment", "comment_reply" -> null

        "post_like_milestone", "post_comment_milestone" -> {
            val thresholdPercent = notification.extra?.percent_threshold
            val activeUsers = notification.extra?.active_users
            if (thresholdPercent != null && activeUsers != null) {
                context.getString(R.string.notification_subtitle_milestone, thresholdPercent)
            } else {
                null
            }
        }

        "story_mention" -> context.getString(R.string.notification_subtitle_story_mention)
        "story_reply" -> {
            val preview = notification.extra?.reply_preview?.trim().orEmpty()
            if (preview.isBlank()) context.getString(R.string.notification_subtitle_story_reply) else null
        }

        else -> null
    }
}

private fun truncateText(value: String, maxLength: Int): String {
    if (value.length <= maxLength) return value
    return value.take(maxLength) + "…"
}

private fun quotedPreview(text: String?, maxLength: Int): String? {
    val value = text?.trim().orEmpty()
    if (value.isBlank()) return null
    return "\"${truncateText(value, maxLength)}\""
}
