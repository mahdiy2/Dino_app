package com.dino.message.corefeature.domain.notification

interface AppNotificationController {
    fun ensureMessageChannel()
    fun ensureActivityChannel()
    fun ensureAppChannels()
    fun clearPrivateChat(username: String)
    fun clearGroupChat(groupId: Int)
    fun clearAll()
    fun clearPrivateMessageDedupe()
    fun disconnectInAppNotifications()
}
