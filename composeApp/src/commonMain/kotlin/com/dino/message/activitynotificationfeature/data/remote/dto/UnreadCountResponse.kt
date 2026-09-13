package com.dino.message.activitynotificationfeature.data.remote.dto

import com.dino.message.activitynotificationfeature.domain.model.UnreadCount
import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import kotlinx.serialization.Serializable

@Serializable
data class UnreadCountResponse(
    val unread_count: Int
) : ResponseToResultMapper<UnreadCount> {
    override fun mapResponseToResult(): UnreadCount =
        UnreadCount(unread_count = unread_count)
}
