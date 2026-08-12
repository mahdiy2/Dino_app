package com.dino.message.corefeature.domain.lifecycle

import com.dino.message.chatfeature.data.notification.ActiveChatTracker
import com.dino.message.chatfeature.data.notification.ActiveGroupTracker
import com.dino.message.corefeature.domain.notification.NotificationSocketController

class AppBackgroundCleanupObserver(
    private val notificationSocketController: NotificationSocketController,
) : AppLifecycleObserver {
    override fun onAppLifecycleEvent(event: AppLifecycleEvent) {
        if (event != AppLifecycleEvent.EnteredBackground) return

        ActiveChatTracker.clearActiveChat()
        ActiveGroupTracker.clearActiveGroup()
        notificationSocketController.disconnect()
    }
}
