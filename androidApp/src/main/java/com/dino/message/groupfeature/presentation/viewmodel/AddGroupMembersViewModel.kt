package com.dino.message.groupfeature.presentation.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.dino.message.R
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.corefeature.domain.resource.StringResolver
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.corefeature.presentation.util.UiEventBus
import com.dino.message.corefeature.presentation.viewmodel.ScopedStateHolder
import com.dino.message.groupfeature.domain.model.MutualFollower
import com.dino.message.groupfeature.domain.usecase.AddGroupMembersUseCase
import com.dino.message.groupfeature.domain.usecase.GetGroupDetailUseCase
import com.dino.message.groupfeature.domain.usecase.GetMutualFollowersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddGroupMembersViewModel constructor(
    private val stringResolver: StringResolver,
    uiEventBus: UiEventBus,
    val groupId: Int,
    initialGroupNameArg: String?,
    private val getMutualFollowersUseCase: GetMutualFollowersUseCase,
    private val getGroupDetailUseCase: GetGroupDetailUseCase,
    private val addGroupMembersUseCase: AddGroupMembersUseCase,
) : ScopedStateHolder(),
    HaveUIEvent by HaveUIEventImpl(uiEventBus) {

    private val initialGroupName: String =
        initialGroupNameArg ?: stringResolver.resolveStringId(R.string.group_fallback_name)

    val searchQuery = mutableStateOf("")

    private val _groupName = MutableStateFlow(initialGroupName)
    val groupName: StateFlow<String> = _groupName.asStateFlow()

    private val _mutualFollowers = mutableStateListOf<MutualFollower>()
    private val _selectedUsernames = mutableStateListOf<String>()
    private val existingMemberUsernames = mutableSetOf<String>()

    val selectedUsernames: List<String> get() = _selectedUsernames

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _membersAdded = MutableStateFlow(false)
    val membersAdded: StateFlow<Boolean> = _membersAdded.asStateFlow()

    private var loadingFollowers = false
    private var loadingGroupDetail = false

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        loadMutualFollowers()
        loadGroupDetail()
    }

    private fun updateLoadingState() {
        _isLoading.value = loadingFollowers || loadingGroupDetail
    }

    private fun loadMutualFollowers() = viewModelScope.launch {
        getMutualFollowersUseCase.execute(page = 1, limit = 100).collect { result ->
            when (result) {
                is Resource.Loading -> {
                    loadingFollowers = true
                    updateLoadingState()
                }
                is Resource.Success -> {
                    loadingFollowers = false
                    _mutualFollowers.clear()
                    _mutualFollowers.addAll(result.data.users)
                    updateLoadingState()
                }
                is Resource.Error -> {
                    loadingFollowers = false
                    _errorMessage.value = result.message.resolveWith(stringResolver)
                    updateLoadingState()
                }
            }
        }
    }

    private fun loadGroupDetail() = viewModelScope.launch {
        getGroupDetailUseCase.execute(groupId).collect { result ->
            when (result) {
                is Resource.Loading -> {
                    loadingGroupDetail = true
                    updateLoadingState()
                }
                is Resource.Success -> {
                    loadingGroupDetail = false
                    _groupName.value = result.data.name
                    existingMemberUsernames.clear()
                    result.data.members.orEmpty().forEach { existingMemberUsernames.add(it.username) }
                    _selectedUsernames.removeAll { username -> existingMemberUsernames.contains(username) }
                    updateLoadingState()
                }
                is Resource.Error -> {
                    loadingGroupDetail = false
                    _errorMessage.value = result.message.resolveWith(stringResolver)
                    updateLoadingState()
                }
            }
        }
    }

    fun toggleMember(username: String) {
        if (existingMemberUsernames.contains(username)) return
        if (_selectedUsernames.contains(username)) {
            _selectedUsernames.remove(username)
        } else {
            _selectedUsernames.add(username)
        }
    }

    fun filteredFollowers(): List<MutualFollower> {
        val query = searchQuery.value.trim().lowercase()
        return _mutualFollowers
            .asSequence()
            .filterNot { existingMemberUsernames.contains(it.username) }
            .filter {
                query.isBlank() ||
                    it.username.lowercase().contains(query) ||
                    it.name.lowercase().contains(query)
            }
            .toList()
    }

    fun addSelectedMembers() {
        val selected = _selectedUsernames.toList()
        if (selected.isEmpty()) {
            _errorMessage.value =
                stringResolver.resolveStringId(R.string.group_add_members_error_select_one)
            return
        }

        viewModelScope.launch {
            addGroupMembersUseCase.execute(groupId, selected).collect { result ->
                when (result) {
                    is Resource.Loading -> _isSubmitting.value = true
                    is Resource.Success -> {
                        _isSubmitting.value = false
                        if (result.data.added.isEmpty()) {
                            _errorMessage.value = stringResolver.resolveStringId(R.string.group_add_members_error_already_member)
                            return@collect
                        }
                        _membersAdded.value = true
                    }
                    is Resource.Error -> {
                        _isSubmitting.value = false
                        _errorMessage.value = result.message.resolveWith(stringResolver)
                    }
                }
            }
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun navigateUp() {
        navigateUp(viewModelScope)
    }
}
