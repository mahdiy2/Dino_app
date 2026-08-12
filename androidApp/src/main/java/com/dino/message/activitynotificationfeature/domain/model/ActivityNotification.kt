package com.dino.message.activitynotificationfeature.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ActivityNotification(
    val id: Long,
    val kind: String,
    val actor: ActivityNotificationActor,
    val target_type: String? = null,
    val target_id: Long? = null,
    val extra: ActivityNotificationExtra? = null,
    val is_read: Boolean,
    val created_at: String?
)
