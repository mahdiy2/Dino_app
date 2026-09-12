package com.dino.message.groupfeature.presentation.viewmodel

import com.dino.message.corefeature.domain.session.AuthSessionStore
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.corefeature.domain.resource.StringResolver
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.corefeature.presentation.util.UiEventBus
import com.dino.message.corefeature.presentation.viewmodel.ScopedStateHolder
import com.dino.message.groupfeature.domain.model.GroupMember
import com.dino.message.groupfeature.domain.usecase.GetGroupDetailUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.dino.message.corefeature.presentation.navigation.AppRoute

class GroupMembersViewModel constructor(
    private val stringResolver: StringResolver,
    uiEventBus: UiEventBus,
    private val groupId: Int,
    private val initialGroupName: String?,
    private val getGroupDetailUseCase: GetGroupDetailUseCase,
    private val sPrefManager: AuthSessionStore,
) : ScopedStateHolder(),
    HaveUIEvent by HaveUIEventImpl(uiEventBus) {

    private val currentUsername: String = sPrefManager.getUsername() ?: ""

    private val _groupName = MutableStateFlow(initialGroupName.orEmpty())
    val groupName: StateFlow<String> = _groupName.asStateFlow()

    private val _members = MutableStateFlow<List<GroupMember>>(emptyList())
    val members: StateFlow<List<GroupMember>> = _members.asStateFlow()

    private val _creatorUsername = MutableStateFlow("")
    val creatorUsername: StateFlow<String> = _creatorUsername.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _canManageMembers = MutableStateFlow(false)
    val canManageMembers: StateFlow<Boolean> = _canManageMembers.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadMembers()
    }

    fun loadMembers() = viewModelScope.launch {
        getGroupDetailUseCase.execute(groupId).collect { result ->
            when (result) {
                is Resource.Loading -> _isLoading.value = true
                is Resource.Success -> {
                    _isLoading.value = false
                    _groupName.value = result.data.name
                    _members.value = result.data.members.orEmpty()
                    _creatorUsername.value = result.data.creator.username
                    _canManageMembers.value =
                        !result.data.isPublicRoom && result.data.creator.username == currentUsername
                }
                is Resource.Error -> {
                    _isLoading.value = false
                    _errorMessage.value = result.message.resolveWith(stringResolver)
                }
            }
        }
    }

    fun openAddMembersScreen() {
        navigateToDestination(
            AppRoute.AddGroupMembers(
                groupId = groupId,
                groupName = _groupName.value.ifBlank { initialGroupName.orEmpty() },
            ),
            viewModelScope,
        )
    }

    fun openMemberProfile(username: String) {
        val normalizedUsername = username.trim()
        if (normalizedUsername.isBlank()) return
        navigateToDestination(
            AppRoute.Profile(username = normalizedUsername),
            viewModelScope,
        )
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun navigateUp() {
        navigateUp(viewModelScope)
    }
}
