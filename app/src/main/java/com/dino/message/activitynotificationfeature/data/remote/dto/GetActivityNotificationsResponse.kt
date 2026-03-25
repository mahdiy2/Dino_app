package com.dino.message.activitynotificationfeature.data.remote.dto

import com.dino.message.activitynotificationfeature.domain.model.ActivityNotification
import com.dino.message.activitynotificationfeature.domain.model.ActivityNotificationsPage
import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import kotlinx.serialization.Serializable

@Serializable
data class GetActivityNotificationsResponse(
    val page: Int,
    val limit: Int,
    val total: Int,
    val notifications: List<ActivityNotification>
) : ResponseToResultMapper<ActivityNotificationsPage> {
    override fun mapResponseToResult(): ActivityNotificationsPage =
        ActivityNotificationsPage(
            page = page,
            limit = limit,
            total = total,
            notifications = notifications
        )
}
