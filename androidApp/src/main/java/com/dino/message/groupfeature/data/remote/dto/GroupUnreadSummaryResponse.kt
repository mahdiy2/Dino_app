package com.dino.message.groupfeature.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupUnreadSummaryResponse(
    val total: Int,
    val groups: List<GroupUnreadItemDto>
)

@Serializable
data class GroupUnreadItemDto(
    @SerialName("group_id") val groupId: Int,
    @SerialName("group_name") val groupName: String,
    val count: Int,
    @SerialName("last_type") val lastType: String,
    @SerialName("last_timestamp") val lastTimestamp: String,
    @SerialName("last_sender") val lastSender: String,
)
