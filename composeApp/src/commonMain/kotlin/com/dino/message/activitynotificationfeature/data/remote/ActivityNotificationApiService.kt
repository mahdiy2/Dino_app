package com.dino.message.activitynotificationfeature.data.remote

import com.dino.message.activitynotificationfeature.domain.model.ActivityNotificationsPage
import com.dino.message.activitynotificationfeature.domain.model.MarkReadResult
import com.dino.message.activitynotificationfeature.domain.model.UnreadCount
import com.dino.message.corefeature.domain.result.Resource

interface ActivityNotificationApiService {
    suspend fun getNotifications(page: Int, limit: Int): Resource<ActivityNotificationsPage>
    suspend fun getUnreadCount(): Resource<UnreadCount>
    suspend fun markRead(notificationIds: List<Long>? = null): Resource<MarkReadResult>
}
