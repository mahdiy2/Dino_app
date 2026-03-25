package com.dino.message.activitynotificationfeature.data.remote

import com.dino.message.corefeature.data.remote.BaseRoute

sealed class ActivityNotificationRoute(url: String) : BaseRoute(url) {
    data class GetNotifications(val page: Int, val limit: Int) :
        ActivityNotificationRoute("api/activity-notifications?page=$page&limit=$limit")

    object UnreadCount :
        ActivityNotificationRoute("api/activity-notifications/unread-count")

    object MarkRead :
        ActivityNotificationRoute("api/activity-notifications/mark-read")
}
