package com.dino.message.chatfeature.data.notification

import com.dino.message.corefeature.domain.session.ActiveChatState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ActiveChatTracker : ActiveChatState {
    private val _activeChatId = MutableStateFlow<String?>(null)
    override val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    override fun setActiveChat(chatId: String?) {
        _activeChatId.value = chatId
    }

    override fun clearActiveChat() {
        _activeChatId.value = null
    }

    override fun isChattingWith(username: String): Boolean {
        return _activeChatId.value == username
    }
}
