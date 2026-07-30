package com.dino.message.profilefeature.presentation.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.profilefeature.domain.model.ProfileUserSummary
import com.dino.message.profilefeature.domain.usecase.GetBlockedUsersUseCase
import com.dino.message.profilefeature.domain.usecase.UnblockUserUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BlockListViewModel constructor(
    app: Context,
    private val getBlockedUsersUseCase: GetBlockedUsersUseCase,
    private val unblockUserUseCase: UnblockUserUseCase
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    val users = mutableStateListOf<ProfileUserSummary>()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _actionUsername = MutableStateFlow<String?>(null)
    val actionUsername: StateFlow<String?> = _actionUsername.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    fun refresh() = viewModelScope.launch {
        getBlockedUsersUseCase.execute(page = 1, limit = 100).collect { res ->
            when (res) {
                is Resource.Success -> {
                    users.clear()
                    users.addAll(res.data.users)
                    _isLoading.value = false
                    _error.value = null
                }

                is Resource.Loading -> {
                    _isLoading.value = true
                }

                is Resource.Error -> {
                    _isLoading.value = false
                    _error.value = res.message.asString(getApplication())
                }
            }
        }
    }

    fun unblock(username: String) = viewModelScope.launch {
        if (_actionUsername.value != null) return@launch
        _actionUsername.value = username

        unblockUserUseCase.execute(username).collect { res ->
            when (res) {
                is Resource.Success -> {
                    _actionUsername.value = null
                    _feedbackMessage.value = res.data.message
                    users.removeAll { it.username.equals(username, ignoreCase = true) }
                    _error.value = null
                }

                is Resource.Loading -> Unit

                is Resource.Error -> {
                    _actionUsername.value = null
                    _error.value = res.message.asString(getApplication())
                }
            }
        }
    }

    fun clearFeedbackMessage() {
        _feedbackMessage.value = null
    }

    fun navigateUp() {
        navigateUp(viewModelScope)
    }
}
