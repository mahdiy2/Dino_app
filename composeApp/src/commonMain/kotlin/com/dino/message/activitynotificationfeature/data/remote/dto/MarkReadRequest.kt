package com.dino.message.activitynotificationfeature.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MarkReadRequest(
    val notification_ids: List<Long>? = null
)
