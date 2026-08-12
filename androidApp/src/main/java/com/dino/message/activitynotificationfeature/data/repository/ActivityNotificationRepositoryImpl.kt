package com.dino.message.activitynotificationfeature.data.repository

import com.dino.message.activitynotificationfeature.data.remote.ActivityNotificationApiService
import com.dino.message.activitynotificationfeature.domain.model.ActivityNotificationsPage
import com.dino.message.activitynotificationfeature.domain.model.MarkReadResult
import com.dino.message.activitynotificationfeature.domain.model.UnreadCount
import com.dino.message.activitynotificationfeature.domain.repository.ActivityNotificationRepository
import com.dino.message.corefeature.domain.result.Resource

class ActivityNotificationRepositoryImpl(
    private val apiService: ActivityNotificationApiService
) : ActivityNotificationRepository {

    override suspend fun getNotifications(
        page: Int,
        limit: Int
    ): Resource<ActivityNotificationsPage> =
        apiService.getNotifications(page = page, limit = limit)

    override suspend fun getUnreadCount(): Resource<UnreadCount> =
        apiService.getUnreadCount()

    override suspend fun markRead(notificationIds: List<Long>?): Resource<MarkReadResult> =
        apiService.markRead(notificationIds = notificationIds)
}
