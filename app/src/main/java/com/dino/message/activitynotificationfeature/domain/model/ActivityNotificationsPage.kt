package com.dino.message.activitynotificationfeature.domain.model

data class ActivityNotificationsPage(
    val page: Int,
    val limit: Int,
    val total: Int,
    val notifications: List<ActivityNotification>
)
