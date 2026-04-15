package com.dino.message.activitynotificationfeature.presentation.component

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dino.message.R
import com.dino.message.activitynotificationfeature.domain.model.ActivityNotification
import com.dino.message.corefeature.presentation.ui.theme.DPLarge
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.util.getRelativeTimeLabel
import com.dino.message.profilefeature.presentation.component.ProfileAvatarMedia

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
    onToggleReplyComposer: () -> Unit,
    onReplyDraftChanged: (String) -> Unit,
    onSendReply: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (!notification.is_read) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = DPExtraLarge, vertical = DPLarge),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DPLarge)
    ) {
        ProfileAvatarMedia(
            modifier = Modifier
                .size(44.dp)
                .clickable { onOpenProfile(notification.actor.username) },
            profileImageUrl = notification.actor.profile_image_url,
            profileVideoUrl = null
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = buildNotificationTitle(notification),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (!notification.is_read) FontWeight.SemiBold else FontWeight.Normal
                ),
                modifier = Modifier.clickable { onOpenProfile(notification.actor.username) },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            val subtitle = buildNotificationSubtitle(notification)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                        Text(text = "Follow Back")
                    }
                }
            }

            if (showReplyButton) {
                TextButton(onClick = onToggleReplyComposer) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Reply,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(text = if (isReplyComposerVisible) "Cancel" else "Reply")
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
                                "Write a reply...",
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
                                contentDescription = "Send reply",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildNotificationTitle(notification: ActivityNotification): String {
    val actorName = notification.actor.name ?: notification.actor.username
    return when (notification.kind) {
        "follow" -> "$actorName started following you"
        "unfollow" -> "$actorName unfollowed you"
        "comment" -> {
            val postPreview = notification.extra?.post_text_preview?.let {
                if (it.length > 40) "\"${it.take(40)}…\"" else "\"$it\""
            } ?: "your post"
            "$actorName commented on $postPreview"
        }
        "comment_reply" -> {
            val postPreview = notification.extra?.post_text_preview?.let {
                if (it.length > 40) "\"${it.take(40)}…\"" else "\"$it\""
            } ?: "your post"
            "$actorName replied to your comment on $postPreview"
        }
        "vote" -> {
            val label = notification.extra?.vote_label ?: "voted on"
            val postPreview = notification.extra?.post_text_preview?.let {
                if (it.length > 40) "\"${it.take(40)}…\"" else "\"$it\""
            } ?: "your post"
            "$actorName ${label}d $postPreview"
        }
        "post_like_milestone" -> {
            val count = notification.extra?.engagement_count ?: notification.extra?.milestone_count
            val postPreview = notification.extra?.post_text_preview?.let {
                if (it.length > 40) "\"${it.take(40)}…\"" else "\"$it\""
            } ?: "your post"
            if (count != null) {
                "Your post $postPreview has received $count likes"
            } else {
                "Your post $postPreview has received many likes"
            }
        }
        "post_comment_milestone" -> {
            val count = notification.extra?.engagement_count ?: notification.extra?.milestone_count
            val postPreview = notification.extra?.post_text_preview?.let {
                if (it.length > 40) "\"${it.take(40)}…\"" else "\"$it\""
            } ?: "your post"
            if (count != null) {
                "Your post $postPreview has received comments from $count people"
            } else {
                "Your post $postPreview has received many comments"
            }
        }
        else -> "$actorName interacted with you"
    }
}

private fun buildNotificationSubtitle(notification: ActivityNotification): String? {
    return when (notification.kind) {
        "comment" -> {
            val commentPreview = notification.extra?.comment_preview?.trim().orEmpty()
            val postPreview = notification.extra?.post_text_preview?.trim().orEmpty()
            when {
                commentPreview.isNotBlank() && postPreview.isNotBlank() ->
                    "Comment: \"$commentPreview\"\nPost: \"${postPreview.take(80)}${if (postPreview.length > 80) "…" else ""}\""
                commentPreview.isNotBlank() -> "Comment: \"$commentPreview\""
                postPreview.isNotBlank() ->
                    "Post: \"${postPreview.take(80)}${if (postPreview.length > 80) "…" else ""}\""
                else -> null
            }
        }
        "comment_reply" -> {
            val replyPreview = notification.extra?.comment_preview?.trim().orEmpty()
            val parentPreview = notification.extra?.parent_comment_preview?.trim().orEmpty()
            val postPreview = notification.extra?.post_text_preview?.trim().orEmpty()
            val parts = buildList {
                if (replyPreview.isNotBlank()) {
                    add("Reply: \"$replyPreview\"")
                }
                if (parentPreview.isNotBlank()) {
                    add(
                        "On comment: \"${parentPreview.take(80)}" +
                            "${if (parentPreview.length > 80) "…" else ""}\""
                    )
                }
                if (postPreview.isNotBlank()) {
                    add(
                        "Post: \"${postPreview.take(80)}" +
                            "${if (postPreview.length > 80) "…" else ""}\""
                    )
                }
            }
            parts.takeIf { it.isNotEmpty() }?.joinToString("\n")
        }
        "post_like_milestone", "post_comment_milestone" -> {
            val thresholdPercent = notification.extra?.percent_threshold
            val activeUsers = notification.extra?.active_users
            if (thresholdPercent != null && activeUsers != null) {
                "Milestone reached: $thresholdPercent% of active users"
            } else {
                null
            }
        }
        else -> null
    }
}
