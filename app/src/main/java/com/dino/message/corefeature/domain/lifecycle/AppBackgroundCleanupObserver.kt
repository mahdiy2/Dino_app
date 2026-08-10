package com.dino.message.corefeature.domain.lifecycle

import com.dino.message.chatfeature.data.notification.ActiveChatTracker
import com.dino.message.chatfeature.data.notification.ActiveGroupTracker
import com.dino.message.chatfeature.data.notification.InAppNotificationManager

class AppBackgroundCleanupObserver(
    private val inAppNotificationManager: InAppNotificationManager,
) : AppLifecycleObserver {
    override fun onAppLifecycleEvent(event: AppLifecycleEvent) {
        if (event != AppLifecycleEvent.EnteredBackground) return

        ActiveChatTracker.clearActiveChat()
        ActiveGroupTracker.clearActiveGroup()
        inAppNotificationManager.disconnect()
    }
}
