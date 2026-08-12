package com.dino.message.corefeature.data.notification

import com.dino.message.chatfeature.data.notification.InAppNotificationManager
import com.dino.message.corefeature.domain.notification.AppNotificationController
import com.dino.message.corefeature.domain.notification.AppNotificationPresenter

class AndroidAppNotificationController(
    private val notificationPresenter: AppNotificationPresenter,
    private val inAppNotificationManager: InAppNotificationManager,
) : AppNotificationController {
    override fun ensureMessageChannel() {
        notificationPresenter.ensureMessageChannel()
    }

    override fun ensureActivityChannel() {
        notificationPresenter.ensureActivityChannel()
    }

    override fun ensureAppChannels() {
        ensureMessageChannel()
        ensureActivityChannel()
    }

    override fun clearPrivateChat(username: String) {
        notificationPresenter.clearPrivateChat(username)
    }

    override fun clearGroupChat(groupId: Int) {
        notificationPresenter.clearGroupChat(groupId)
    }

    override fun clearAll() {
        notificationPresenter.clearAll()
    }

    override fun clearPrivateMessageDedupe() {
        inAppNotificationManager.clearPrivateMessageNotificationDedupe()
    }

    override fun disconnectInAppNotifications() {
        inAppNotificationManager.disconnect()
    }
}
