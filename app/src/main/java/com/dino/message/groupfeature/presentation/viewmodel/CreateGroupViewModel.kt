package com.dino.message.groupfeature.presentation.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.R
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.groupfeature.domain.model.MutualFollower
import com.dino.message.groupfeature.domain.usecase.CreateGroupUseCase
import com.dino.message.groupfeature.domain.usecase.GetMutualFollowersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    @ApplicationContext app: Context,
    private val createGroupUseCase: CreateGroupUseCase,
    private val getMutualFollowersUseCase: GetMutualFollowersUseCase,
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

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
                    _errorMessage.value = res.message.asString(getApplication<DinoOrderApplication>())
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
                getApplication<DinoOrderApplication>().getString(R.string.group_create_error_empty_name)
            return
        }
        if (name.length > 120) {
            _errorMessage.value =
                getApplication<DinoOrderApplication>().getString(R.string.group_create_error_name_too_long)
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
                        _errorMessage.value = res.message.asString(getApplication<DinoOrderApplication>())
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
