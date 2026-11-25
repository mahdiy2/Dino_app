package com.dino.message.corefeature.data.notification

import com.dino.message.chatfeature.data.notification.InAppNotificationManager
import com.dino.message.chatfeature.data.notification.NotificationHelper
import com.dino.message.corefeature.domain.notification.AppNotificationController

class AndroidAppNotificationController(
    private val notificationHelper: NotificationHelper,
    private val inAppNotificationManager: InAppNotificationManager,
) : AppNotificationController {
    override fun clearPrivateChat(username: String) {
        notificationHelper.clearPrivateChat(username)
    }

    override fun clearGroupChat(groupId: Int) {
        notificationHelper.clearGroupChat(groupId)
    }

    override fun clearAll() {
        notificationHelper.clearAll()
    }

    override fun clearPrivateMessageDedupe() {
        inAppNotificationManager.clearPrivateMessageNotificationDedupe()
    }

    override fun disconnectInAppNotifications() {
        inAppNotificationManager.disconnect()
    }
}
