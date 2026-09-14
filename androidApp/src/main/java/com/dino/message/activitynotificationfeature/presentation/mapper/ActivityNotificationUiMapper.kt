package com.dino.message.activitynotificationfeature.presentation.mapper

import android.content.Context
import com.dino.message.R
import com.dino.message.activitynotificationfeature.domain.model.ActivityNotification
import com.dino.message.activitynotificationfeature.presentation.component.ActivityNotificationUiItem
import com.dino.message.activitynotificationfeature.presentation.component.NotificationDetailLine
import com.dino.message.corefeature.presentation.util.getRelativeTimeLabel

fun ActivityNotification.toUiItem(context: Context): ActivityNotificationUiItem {
    val actorName = actor.name ?: actor.username
    val postPreview = quotedPreview(extra?.post_text_preview, 40)
        ?: context.getString(R.string.notification_your_post)
    val title = when (kind) {
        "follow" -> context.getString(R.string.notification_title_follow, actorName)
        "unfollow" -> context.getString(R.string.notification_title_unfollow, actorName)
        "comment" -> context.getString(R.string.notification_title_comment, actorName, postPreview)
        "comment_reply" -> context.getString(R.string.notification_title_comment_reply, actorName, postPreview)
        "vote" -> context.getString(
            R.string.notification_title_vote,
            actorName,
            extra?.vote_label ?: context.getString(R.string.notification_vote_label_default),
            postPreview,
        )
        "post_like_milestone" -> milestoneTitle(
            context,
            R.string.notification_title_like_milestone_count,
            R.string.notification_title_like_milestone_many,
            postPreview,
            extra?.engagement_count ?: extra?.milestone_count,
        )
        "post_comment_milestone" -> milestoneTitle(
            context,
            R.string.notification_title_comment_milestone_count,
            R.string.notification_title_comment_milestone_many,
            postPreview,
            extra?.engagement_count ?: extra?.milestone_count,
        )
        "story_mention" -> context.getString(R.string.notification_title_story_mention, actorName)
        "story_reply" -> context.getString(R.string.notification_title_story_reply, actorName)
        else -> context.getString(R.string.notification_title_interacted, actorName)
    }

    return ActivityNotificationUiItem(
        actorUsername = actor.username,
        actorName = actor.name,
        actorProfileImageUrl = actor.profile_image_url,
        actorBadge = actor.badge,
        actorProfileImageShape = actor.profile_image_shape,
        isRead = is_read,
        title = title,
        detailLines = details(context),
        subtitle = subtitle(context),
        relativeTime = getRelativeTimeLabel(created_at),
    )
}

private fun ActivityNotification.details(context: Context): List<NotificationDetailLine> = when (kind) {
    "comment" -> buildList {
        extra?.comment_preview?.trim()?.takeIf { it.isNotBlank() }?.let {
            add(NotificationDetailLine(context.getString(R.string.notification_detail_label_comment), truncateText(it, 100)))
        }
        extra?.post_text_preview?.trim()?.takeIf { it.isNotBlank() }?.let {
            add(NotificationDetailLine(context.getString(R.string.notification_detail_label_post), truncateText(it, 100)))
        }
    }
    "comment_reply" -> buildList {
        extra?.comment_preview?.trim()?.takeIf { it.isNotBlank() }?.let {
            add(NotificationDetailLine(context.getString(R.string.notification_detail_label_reply), truncateText(it, 100)))
        }
        extra?.parent_comment_preview?.trim()?.takeIf { it.isNotBlank() }?.let {
            add(NotificationDetailLine(context.getString(R.string.notification_detail_label_on_comment), truncateText(it, 100)))
        }
        extra?.post_text_preview?.trim()?.takeIf { it.isNotBlank() }?.let {
            add(NotificationDetailLine(context.getString(R.string.notification_detail_label_post), truncateText(it, 100)))
        }
    }
    "story_reply" -> extra?.reply_preview?.trim()?.takeIf { it.isNotBlank() }?.let {
        listOf(NotificationDetailLine(context.getString(R.string.notification_detail_label_reply), truncateText(it, 100)))
    } ?: emptyList()
    else -> emptyList()
}

private fun ActivityNotification.subtitle(context: Context): String? = when (kind) {
    "post_like_milestone", "post_comment_milestone" -> {
        val notificationExtra = extra
        if (notificationExtra?.percent_threshold != null && notificationExtra.active_users != null) {
            context.getString(R.string.notification_subtitle_milestone, notificationExtra.percent_threshold)
        } else null
    }
    "story_mention" -> context.getString(R.string.notification_subtitle_story_mention)
    "story_reply" -> if (extra?.reply_preview?.trim().orEmpty().isBlank()) {
        context.getString(R.string.notification_subtitle_story_reply)
    } else null
    else -> null
}

private fun milestoneTitle(
    context: Context,
    countId: Int,
    manyId: Int,
    postPreview: String,
    count: Int?,
): String = if (count != null) {
    context.getString(countId, postPreview, count)
} else {
    context.getString(manyId, postPreview)
}

private fun truncateText(value: String, maxLength: Int): String =
    if (value.length <= maxLength) value else value.take(maxLength) + "…"

private fun quotedPreview(text: String?, maxLength: Int): String? {
    val value = text?.trim().orEmpty()
    return value.takeIf { it.isNotBlank() }?.let { "\"${truncateText(it, maxLength)}\"" }
}
