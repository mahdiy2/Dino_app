package com.dino.message.groupfeature.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.groupfeature.domain.model.GroupMember
import com.dino.message.groupfeature.domain.usecase.GetGroupDetailUseCase
import com.ramcosta.composedestinations.generated.destinations.AddGroupMembersScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ProfileScreenDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupMembersViewModel constructor(
    app: Context,
    savedStateHandle: SavedStateHandle,
    private val getGroupDetailUseCase: GetGroupDetailUseCase,
    private val sPrefManager: SPrefManager,
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    private val groupId: Int = checkNotNull(savedStateHandle["groupId"])
    private val initialGroupName: String = savedStateHandle["groupName"] ?: ""
    private val currentUsername: String = sPrefManager.getUsername() ?: ""

    private val _groupName = MutableStateFlow(initialGroupName)
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
                    _errorMessage.value = result.message.asString(getApplication<DinoOrderApplication>())
                }
            }
        }
    }

    fun openAddMembersScreen() {
        navigateToDestination(
            AddGroupMembersScreenDestination(
                groupId = groupId,
                groupName = _groupName.value.ifBlank { initialGroupName },
            ),
            viewModelScope,
        )
    }

    fun openMemberProfile(username: String) {
        val normalizedUsername = username.trim()
        if (normalizedUsername.isBlank()) return
        navigateToDestination(
            ProfileScreenDestination(username = normalizedUsername),
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
