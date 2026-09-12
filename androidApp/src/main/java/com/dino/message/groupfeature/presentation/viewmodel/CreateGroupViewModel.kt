package com.dino.message.groupfeature.presentation.viewmodel

import com.dino.message.corefeature.presentation.util.asString
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
import com.dino.message.groupfeature.domain.usecase.CreateGroupUseCase
import com.dino.message.groupfeature.domain.usecase.GetMutualFollowersUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreateGroupViewModel constructor(
    private val stringResolver: StringResolver,
    uiEventBus: UiEventBus,
    private val createGroupUseCase: CreateGroupUseCase,
    private val getMutualFollowersUseCase: GetMutualFollowersUseCase,
) : ScopedStateHolder(),
    HaveUIEvent by HaveUIEventImpl(uiEventBus) {

    val groupName = mutableStateOf("")
    val searchQuery = mutableStateOf("")

    private val _mutualFollowers = mutableStateListOf<MutualFollower>()
    val mutualFollowers: List<MutualFollower> get() = _mutualFollowers

    private val _selectedUsernames = mutableStateListOf<String>()
    val selectedUsernames: List<String> get() = _selectedUsernames

    private val _isLoadingFollowers = MutableStateFlow(false)
    val isLoadingFollowers: StateFlow<Boolean> = _isLoadingFollowers.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _groupCreated = MutableStateFlow(false)
    val groupCreated: StateFlow<Boolean> = _groupCreated.asStateFlow()

    init {
        loadMutualFollowers()
    }

    private fun loadMutualFollowers() = viewModelScope.launch {
        getMutualFollowersUseCase.execute(page = 1, limit = 100).collect { res ->
            when (res) {
                is Resource.Loading -> _isLoadingFollowers.value = true
                is Resource.Success -> {
                    _isLoadingFollowers.value = false
                    _mutualFollowers.clear()
                    _mutualFollowers.addAll(res.data.users)
                }
                is Resource.Error -> {
                    _isLoadingFollowers.value = false
                    _errorMessage.value = res.message.resolveWith(stringResolver)
                }
            }
        }
    }

    fun toggleMember(username: String) {
        if (_selectedUsernames.contains(username)) {
            _selectedUsernames.remove(username)
        } else {
            _selectedUsernames.add(username)
        }
    }

    fun createGroup() {
        val name = groupName.value.trim()
        if (name.isBlank()) {
            _errorMessage.value =
                stringResolver.resolveStringId(R.string.group_create_error_empty_name)
            return
        }
        if (name.length > 120) {
            _errorMessage.value =
                stringResolver.resolveStringId(R.string.group_create_error_name_too_long)
            return
        }

        viewModelScope.launch {
            createGroupUseCase.execute(name, _selectedUsernames.toList()).collect { res ->
                when (res) {
                    is Resource.Loading -> _isCreating.value = true
                    is Resource.Success -> {
                        _isCreating.value = false
                        _groupCreated.value = true
                    }
                    is Resource.Error -> {
                        _isCreating.value = false
                        _errorMessage.value = res.message.resolveWith(stringResolver)
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

    fun filteredFollowers(): List<MutualFollower> {
        val query = searchQuery.value.trim().lowercase()
        if (query.isBlank()) return _mutualFollowers
        return _mutualFollowers.filter {
            it.username.lowercase().contains(query) || it.name.lowercase().contains(query)
        }
    }
}
