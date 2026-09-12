package com.dino.message.profilefeature.presentation.viewmodel

import com.dino.message.corefeature.presentation.util.asString
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import com.dino.message.corefeature.domain.session.AuthSessionStore
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.corefeature.domain.resource.StringResolver
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.corefeature.presentation.util.UiEventBus
import com.dino.message.corefeature.presentation.viewmodel.ScopedStateHolder
import com.dino.message.profilefeature.domain.model.ProfileUserSummary
import com.dino.message.profilefeature.domain.usecase.GetFollowersUseCase
import com.dino.message.profilefeature.domain.usecase.GetFollowingUseCase
import com.dino.message.profilefeature.domain.usecase.RemoveFollowerUseCase
import com.dino.message.profilefeature.domain.usecase.UnfollowUserUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.dino.message.corefeature.presentation.navigation.AppRoute

class ProfileFollowListViewModel constructor(
    private val stringResolver: StringResolver,
    uiEventBus: UiEventBus,
    private val sPrefManager: AuthSessionStore,
    private val getFollowersUseCase: GetFollowersUseCase,
    private val getFollowingUseCase: GetFollowingUseCase,
    private val removeFollowerUseCase: RemoveFollowerUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
) : ScopedStateHolder(),
    HaveUIEvent by HaveUIEventImpl(uiEventBus) {

    val users = mutableStateListOf<ProfileUserSummary>()

    private val actionLoadingByUsername = mutableStateMapOf<String, Boolean>()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _title = MutableStateFlow("Followers")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _isOwnProfile = MutableStateFlow(false)
    val isOwnProfile: StateFlow<Boolean> = _isOwnProfile.asStateFlow()
    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    private var targetUsername: String = ""
    private var showFollowers: Boolean = true
    private var initializedKey: String? = null

    private val pageLimit = 30
    private var currentPage = 1
    private var totalUsers = Int.MAX_VALUE
    private var isUsersRequestRunning = false

    fun initialize(username: String, showFollowers: Boolean) {
        val key = "$username#$showFollowers"
        if (initializedKey == key) return
        initializedKey = key

        targetUsername = username.trim()
        this.showFollowers = showFollowers
        _title.value = if (showFollowers) "Followers" else "Following"
        _isOwnProfile.value = targetUsername.equals(sPrefManager.getUsername()?.trim(), ignoreCase = true)
        refresh()
    }

    fun refresh() {
        if (targetUsername.isBlank()) return
        currentPage = 1
        totalUsers = Int.MAX_VALUE
        _totalCount.value = 0
        _hasMore.value = true
        actionLoadingByUsername.clear()
        users.clear()
        requestUsersPage(isRefresh = true)
    }

    fun loadNextPage() {
        if (targetUsername.isBlank()) return
        if (isUsersRequestRunning || !_hasMore.value) return
        requestUsersPage(isRefresh = false)
    }

    private fun requestUsersPage(isRefresh: Boolean) {
        viewModelScope.launch {
            if (isUsersRequestRunning) return@launch
            isUsersRequestRunning = true

            val flow = if (showFollowers) {
                getFollowersUseCase.execute(username = targetUsername, page = currentPage, limit = pageLimit)
            } else {
                getFollowingUseCase.execute(username = targetUsername, page = currentPage, limit = pageLimit)
            }

            flow.collect { res ->
                when (res) {
                    is Resource.Success -> {
                        if (isRefresh) {
                            users.clear()
                        }

                        totalUsers = res.data.total
                        _totalCount.value = totalUsers
                        val existingIds = users.mapTo(mutableSetOf()) { it.id }
                        val incomingUsers = res.data.users.filter { user ->
                            existingIds.add(user.id)
                        }
                        users.addAll(incomingUsers)

                        if (res.data.users.isNotEmpty()) {
                            currentPage = res.data.page + 1
                        }

                        _hasMore.value = users.size < totalUsers && incomingUsers.isNotEmpty()
                        _isLoading.value = false
                        _isLoadingMore.value = false
                        _error.value = null
                        isUsersRequestRunning = false
                    }

                    is Resource.Loading -> {
                        if (isRefresh) {
                            _isLoading.value = true
                        } else {
                            _isLoadingMore.value = true
                        }
                    }

                    is Resource.Error -> {
                        _isLoading.value = false
                        _isLoadingMore.value = false
                        _error.value = res.message.resolveWith(stringResolver)
                        isUsersRequestRunning = false
                    }
                }
            }
        }
    }

    fun isActionLoading(username: String): Boolean = actionLoadingByUsername[username] == true

    fun onUserAction(user: ProfileUserSummary) {
        if (!_isOwnProfile.value) return
        if (isActionLoading(user.username)) return
        if (showFollowers) {
            removeFollower(user.username)
        } else {
            unfollowUser(user.username)
        }
    }

    private fun removeFollower(username: String) {
        runUserAction(
            username = username,
            action = { removeFollowerUseCase.execute(username) },
        )
    }

    private fun unfollowUser(username: String) {
        runUserAction(
            username = username,
            action = { unfollowUserUseCase.execute(username) },
        )
    }

    private fun runUserAction(
        username: String,
        action: () -> kotlinx.coroutines.flow.Flow<Resource<*>>,
    ) {
        viewModelScope.launch {
            action().collect { res ->
                when (res) {
                    is Resource.Success -> {
                        users.removeAll { it.username.equals(username, ignoreCase = true) }
                        totalUsers = (totalUsers - 1).coerceAtLeast(0)
                        _totalCount.value = totalUsers
                        _error.value = null
                        actionLoadingByUsername.remove(username)
                    }

                    is Resource.Loading -> {
                        actionLoadingByUsername[username] = true
                    }

                    is Resource.Error -> {
                        _error.value = res.message.resolveWith(stringResolver)
                        actionLoadingByUsername.remove(username)
                    }
                }
            }
        }
    }

    fun openUserProfile(username: String) {
        navigateToDestination(AppRoute.Profile(username = username), viewModelScope)
    }

    fun navigateUp() {
        navigateUp(viewModelScope)
    }
}
