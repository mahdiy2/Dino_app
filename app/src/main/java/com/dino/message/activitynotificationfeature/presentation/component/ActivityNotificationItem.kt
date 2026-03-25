package com.dino.message.activitynotificationfeature.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dino.message.activitynotificationfeature.domain.model.ActivityNotification
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPLarge
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.profilefeature.presentation.component.ProfileAvatarMedia

@Composable
fun ActivityNotificationItem(
    notification: ActivityNotification,
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
            modifier = Modifier.size(44.dp),
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
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            val subtitle = buildNotificationSubtitle(notification)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (notification.created_at != null) {
                Text(
                    text = notification.created_at,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
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
        "vote" -> {
            val label = notification.extra?.vote_label ?: "voted on"
            val postPreview = notification.extra?.post_text_preview?.let {
                if (it.length > 40) "\"${it.take(40)}…\"" else "\"$it\""
            } ?: "your post"
            "$actorName ${label}d $postPreview"
        }
        else -> "$actorName interacted with you"
    }
}

private fun buildNotificationSubtitle(notification: ActivityNotification): String? {
    return when (notification.kind) {
        "comment" -> notification.extra?.comment_preview
        else -> null
    }
}
