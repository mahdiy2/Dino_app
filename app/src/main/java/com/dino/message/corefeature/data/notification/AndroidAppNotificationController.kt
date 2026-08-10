package com.dino.message.corefeature.data.notification

import android.content.Context
import com.dino.message.chatfeature.data.notification.InAppNotificationManager
import com.dino.message.chatfeature.data.notification.NotificationHelper
import com.dino.message.corefeature.domain.notification.AppNotificationController

class AndroidAppNotificationController(
    private val context: Context,
    private val inAppNotificationManager: InAppNotificationManager,
) : AppNotificationController {
    override fun ensureMessageChannel() {
        NotificationHelper.ensureChannel(context)
    }

    override fun ensureActivityChannel() {
        NotificationHelper.ensureActivityChannel(context)
    }

    override fun ensureAppChannels() {
        ensureMessageChannel()
        ensureActivityChannel()
    }

    override fun clearPrivateChat(username: String) {
        NotificationHelper.clearNotificationForUser(context, username)
    }

    override fun clearGroupChat(groupId: Int) {
        NotificationHelper.clearNotificationForGroup(context, groupId)
    }

    override fun clearAll() {
        NotificationHelper.clearAllNotifications(context)
    }

    override fun clearPrivateMessageDedupe() {
        inAppNotificationManager.clearPrivateMessageNotificationDedupe()
    }

    override fun disconnectInAppNotifications() {
        inAppNotificationManager.disconnect()
    }
}
