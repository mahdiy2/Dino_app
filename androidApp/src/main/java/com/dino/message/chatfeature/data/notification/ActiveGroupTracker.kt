package com.dino.message.chatfeature.data.notification

import com.dino.message.corefeature.domain.session.ActiveGroupState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ActiveGroupTracker : ActiveGroupState {
    private val _activeGroupId = MutableStateFlow<Int?>(null)
    override val activeGroupId: StateFlow<Int?> = _activeGroupId.asStateFlow()

    override fun setActiveGroup(groupId: Int?) {
        _activeGroupId.value = groupId
    }

    override fun clearActiveGroup() {
        _activeGroupId.value = null
    }

    override fun isViewingGroup(groupId: Int): Boolean {
        return _activeGroupId.value == groupId
    }
}
