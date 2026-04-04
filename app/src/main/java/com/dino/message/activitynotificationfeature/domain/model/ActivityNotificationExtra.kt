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
    val parent_comment_id: Long? = null
)
