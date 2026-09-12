package com.dino.message.corefeature.domain.session

import kotlinx.coroutines.flow.StateFlow

interface ActiveChatState {
    val activeChatId: StateFlow<String?>

    fun setActiveChat(chatId: String?)
    fun clearActiveChat()
    fun isChattingWith(username: String): Boolean
}

interface ActiveGroupState {
    val activeGroupId: StateFlow<Int?>

    fun setActiveGroup(groupId: Int?)
    fun clearActiveGroup()
    fun isViewingGroup(groupId: Int): Boolean
}
