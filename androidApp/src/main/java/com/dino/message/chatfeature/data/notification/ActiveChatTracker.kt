package com.dino.message.chatfeature.data.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ActiveChatTracker {
    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    fun setActiveChat(chatId: String?) {
        _activeChatId.value = chatId
    }

    fun clearActiveChat() {
        _activeChatId.value = null
    }

    fun isChattingWith(username: String): Boolean {
        return _activeChatId.value == username
    }
}
