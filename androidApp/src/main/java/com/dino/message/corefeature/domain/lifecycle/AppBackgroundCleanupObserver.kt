package com.dino.message.corefeature.domain.lifecycle

import com.dino.message.corefeature.domain.notification.NotificationSocketController
import com.dino.message.corefeature.domain.session.ActiveConversationStateCleaner

class AppBackgroundCleanupObserver(
    private val notificationSocketController: NotificationSocketController,
    private val activeConversationStateCleaner: ActiveConversationStateCleaner,
) : AppLifecycleObserver {
    override fun onAppLifecycleEvent(event: AppLifecycleEvent) {
        if (event != AppLifecycleEvent.EnteredBackground) return

        activeConversationStateCleaner.clearActiveConversations()
        notificationSocketController.disconnect()
    }
}
