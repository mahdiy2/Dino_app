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
import com.dino.message.profilefeature.domain.usecase.GetFollowersUseCase
import com.dino.message.profilefeature.domain.usecase.GetFollowingUseCase
import com.ramcosta.composedestinations.generated.destinations.ProfileScreenDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileFollowListViewModel @Inject constructor(
    @ApplicationContext app: Context,
    private val getFollowersUseCase: GetFollowersUseCase,
    private val getFollowingUseCase: GetFollowingUseCase
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    val users = mutableStateListOf<ProfileUserSummary>()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _title = MutableStateFlow("Followers")
    val title: StateFlow<String> = _title.asStateFlow()

    private var targetUsername: String = ""
    private var showFollowers: Boolean = true
    private var initializedKey: String? = null

    fun initialize(username: String, showFollowers: Boolean) {
        val key = "$username#$showFollowers"
        if (initializedKey == key) return
        initializedKey = key

        targetUsername = username
        this.showFollowers = showFollowers
        _title.value = if (showFollowers) "Followers" else "Following"
        refresh()
    }

    fun refresh() {
        if (targetUsername.isBlank()) return
        viewModelScope.launch {
            val flow = if (showFollowers) {
                getFollowersUseCase.execute(username = targetUsername, page = 1, limit = 50)
            } else {
                getFollowingUseCase.execute(username = targetUsername, page = 1, limit = 50)
            }

            flow.collect { res ->
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
    }

    fun openUserProfile(username: String) {
        navigateToDestination(ProfileScreenDestination(username = username), viewModelScope)
    }

    fun navigateUp() {
        navigateUp(viewModelScope)
    }
}
