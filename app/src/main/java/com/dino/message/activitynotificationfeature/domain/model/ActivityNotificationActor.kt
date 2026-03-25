package com.dino.message.activitynotificationfeature.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ActivityNotificationActor(
    val id: Long,
    val username: String,
    val name: String?,
    val profile_image_url: String?
)
