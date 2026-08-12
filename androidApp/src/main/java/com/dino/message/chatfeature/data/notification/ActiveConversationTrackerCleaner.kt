package com.dino.message.chatfeature.data.notification

import com.dino.message.corefeature.domain.session.ActiveConversationStateCleaner

class ActiveConversationTrackerCleaner : ActiveConversationStateCleaner {
    override fun clearActiveConversations() {
        ActiveChatTracker.clearActiveChat()
        ActiveGroupTracker.clearActiveGroup()
    }
}
