package com.dino.message.activitynotificationfeature.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ActivityNotificationExtra(
    val comment_preview: String? = null,
    val post_text_preview: String? = null,
    val vote_value: Int? = null,
    val vote_label: String? = null,
    val post_id: Long? = null,
    val comment_id: Long? = null,
    val parent_comment_id: Long? = null,
    val parent_comment_preview: String? = null,
    val engagement_type: String? = null,
    val engagement_count: Int? = null,
    val milestone_count: Int? = null,
    val threshold: Int? = null,
    val percent_threshold: Int? = null,
    val active_users: Int? = null
)
