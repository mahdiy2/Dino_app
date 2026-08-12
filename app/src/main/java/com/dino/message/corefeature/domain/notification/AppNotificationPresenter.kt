package com.dino.message.corefeature.domain.notification

interface AppNotificationPresenter {
    fun ensureMessageChannel()
    fun ensureActivityChannel()
    fun showPrivateMessage(senderUsername: String, body: String)
    fun showGroupMessage(groupId: Int, groupName: String, body: String)
    fun showActivity(title: String, body: String, notificationId: Int = DEFAULT_ACTIVITY_NOTIFICATION_ID)
    fun clearPrivateChat(username: String)
    fun clearGroupChat(groupId: Int)
    fun clearAll()

    companion object {
        val DEFAULT_ACTIVITY_NOTIFICATION_ID: Int = "activity_notif".hashCode()
    }
}
