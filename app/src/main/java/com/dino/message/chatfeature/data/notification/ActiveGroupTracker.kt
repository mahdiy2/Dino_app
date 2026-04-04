package com.dino.message.chatfeature.data.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ActiveGroupTracker {
    private val _activeGroupId = MutableStateFlow<Int?>(null)
    val activeGroupId: StateFlow<Int?> = _activeGroupId.asStateFlow()

    fun setActiveGroup(groupId: Int?) {
        _activeGroupId.value = groupId
    }

    fun clearActiveGroup() {
        _activeGroupId.value = null
    }

    fun isViewingGroup(groupId: Int): Boolean {
        return _activeGroupId.value == groupId
    }
}
