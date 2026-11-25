package com.dino.message.corefeature.domain.notification

sealed interface NotificationPayload {
    val body: String

    data class PrivateMessage(
        val senderUsername: String,
        override val body: String,
    ) : NotificationPayload

    data class GroupMessage(
        val groupId: Int,
        val groupName: String,
        override val body: String,
    ) : NotificationPayload

    data class Activity(
        val title: String,
        override val body: String,
        val eventId: Long? = null,
    ) : NotificationPayload
}
