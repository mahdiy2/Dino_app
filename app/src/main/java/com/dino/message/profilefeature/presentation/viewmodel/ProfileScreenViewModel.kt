package com.dino.message.profilefeature.presentation.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.dino.message.chatfeature.data.worker.UnreadPollingScheduler
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.presentation.util.PopBackStackLevel
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.profilefeature.domain.model.Profile
import com.dino.message.profilefeature.domain.usecase.FollowUserUseCase
import com.dino.message.profilefeature.domain.usecase.GetFollowStatusUseCase
import com.dino.message.profilefeature.domain.usecase.GetMyProfileUseCase
import com.dino.message.profilefeature.domain.usecase.GetProfilePostsUseCase
import com.dino.message.profilefeature.domain.usecase.GetProfileUseCase
import com.dino.message.profilefeature.domain.usecase.UnfollowUserUseCase
import com.dino.message.threadfeature.domain.model.Post
import com.dino.message.threadfeature.domain.model.VoteTargetType
import com.dino.message.threadfeature.domain.usecase.VoteUseCase
import com.ramcosta.composedestinations.generated.destinations.EditProfileScreenDestination
import com.ramcosta.composedestinations.generated.destinations.LoginScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ProfileFollowListScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ThreadCommentsBottomSheetScreenDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileScreenViewModel @Inject constructor(
    @ApplicationContext app: Context,
    private val sPrefManager: SPrefManager,
    private val getMyProfileUseCase: GetMyProfileUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val getProfilePostsUseCase: GetProfilePostsUseCase,
    private val getFollowStatusUseCase: GetFollowStatusUseCase,
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val voteUseCase: VoteUseCase
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {
    val profile = mutableStateOf<Profile?>(null)
    val posts = mutableStateListOf<Post>()

    private val _isProfileLoading = MutableStateFlow(false)
    val isProfileLoading: StateFlow<Boolean> = _isProfileLoading.asStateFlow()

    private val _isPostsLoading = MutableStateFlow(false)
    val isPostsLoading: StateFlow<Boolean> = _isPostsLoading.asStateFlow()

    private val _isActionLoading = MutableStateFlow(false)
    val isActionLoading: StateFlow<Boolean> = _isActionLoading.asStateFlow()

    private val _isOwnProfile = MutableStateFlow(true)
    val isOwnProfile: StateFlow<Boolean> = _isOwnProfile.asStateFlow()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val currentUsername = sPrefManager.getUsername().toString().trim()
    private var requestedUsername: String? = null
    private var initializedKey: String? = null
    private var shouldRefreshWhenScreenResumes = false

    private val postVoteByPost = mutableStateMapOf<Long, Long>()
    private val postErrorByPost = mutableStateMapOf<Long, String?>()

    fun initialize(profileUsername: String?) {
        val key = profileUsername?.trim()?.ifEmpty { null }
        if (initializedKey == key) return
        initializedKey = key

        requestedUsername = key
        _isOwnProfile.value = key == null || key.equals(currentUsername, ignoreCase = true)
        _isFollowing.value = false
        refresh()
    }

    fun refresh() {
        loadProfile()
        loadPosts()
        loadFollowStatus()
    }

    private fun loadProfile() = viewModelScope.launch {
        val call = if (_isOwnProfile.value) {
            getMyProfileUseCase.execute()
        } else {
            getProfileUseCase.execute(targetUsername())
        }

        call.collect { res ->
            when (res) {
                is Resource.Success -> {
                    profile.value = res.data
                    _error.value = null
                    _isProfileLoading.value = false
                }

                is Resource.Loading -> {
                    _isProfileLoading.value = true
                }

                is Resource.Error -> {
                    _error.value = res.message.asString(getApplication())
                    _isProfileLoading.value = false
                }
            }
        }
    }

    private fun loadPosts() = viewModelScope.launch {
        getProfilePostsUseCase.execute(
            username = targetUsername(),
            page = 1,
            limit = 20
        ).collect { res ->
            when (res) {
                is Resource.Success -> {
                    posts.clear()
                    posts.addAll(res.data.posts)
                    _isPostsLoading.value = false
                }

                is Resource.Loading -> {
                    _isPostsLoading.value = true
                }

                is Resource.Error -> {
                    _isPostsLoading.value = false
                    if (_error.value == null) {
                        _error.value = res.message.asString(getApplication())
                    }
                }
            }
        }
    }

    private fun loadFollowStatus() {
        if (_isOwnProfile.value) {
            _isFollowing.value = false
            return
        }

        viewModelScope.launch {
            getFollowStatusUseCase.execute(targetUsername()).collect { res ->
                when (res) {
                    is Resource.Success -> {
                        _isFollowing.value = res.data
                    }

                    is Resource.Loading -> Unit

                    is Resource.Error -> {
                        if (_error.value == null) {
                            _error.value = res.message.asString(getApplication())
                        }
                    }
                }
            }
        }
    }

    fun toggleFollow() {
        if (_isOwnProfile.value || _isActionLoading.value) return

        val username = targetUsername()
        val currentFollow = _isFollowing.value
        val actionFlow = if (currentFollow) {
            unfollowUserUseCase.execute(username)
        } else {
            followUserUseCase.execute(username)
        }

        viewModelScope.launch {
            actionFlow.collect { res ->
                when (res) {
                    is Resource.Success -> {
                        _isActionLoading.value = false
                        _error.value = null
                        val message = res.data.message.lowercase()
                        _isFollowing.value = when {
                            message.contains("already following") -> true
                            message.contains("followed") -> true
                            message.contains("not following") -> false
                            message.contains("unfollowed") -> false
                            else -> !currentFollow
                        }
                    }

                    is Resource.Loading -> {
                        _isActionLoading.value = true
                    }

                    is Resource.Error -> {
                        _isActionLoading.value = false
                        _error.value = res.message.asString(getApplication())
                    }
                }
            }
        }
    }

    fun openEditProfileScreen() {
        if (!_isOwnProfile.value || _isActionLoading.value) return
        shouldRefreshWhenScreenResumes = true
        navigateToDestination(EditProfileScreenDestination, viewModelScope)
    }

    fun logout(){
        UnreadPollingScheduler.cancel(getApplication<DinoOrderApplication>())
        getApplication<DinoOrderApplication>()
            .inAppNotificationManager
            .disconnect()
        navigateWithPopBackStackToDestination(
            LoginScreenDestination,
            viewModelScope,
            PopBackStackLevel.ALL
        )
    }

    fun refreshIfNeededAfterEditProfile() {
        if (!shouldRefreshWhenScreenResumes) return
        shouldRefreshWhenScreenResumes = false
        refresh()
    }

    fun openFollowersList() {
        navigateToDestination(
            ProfileFollowListScreenDestination(
                username = targetUsername(),
                showFollowers = true
            ),
            viewModelScope
        )
    }

    fun openFollowingList() {
        navigateToDestination(
            ProfileFollowListScreenDestination(
                username = targetUsername(),
                showFollowers = false
            ),
            viewModelScope
        )
    }

    fun selectedPostVote(postId: Long): Long = postVoteByPost[postId] ?: 0L

    fun postError(postId: Long): String? = postErrorByPost[postId]

    fun votePost(postId: Long, value: Long) = viewModelScope.launch {
        voteUseCase.execute(
            targetType = VoteTargetType.POST,
            targetId = postId,
            value = value
        ).collect { res ->
            when (res) {
                is Resource.Success -> {
                    postVoteByPost[postId] = value
                    postErrorByPost[postId] = null
                }

                is Resource.Loading -> Unit

                is Resource.Error -> {
                    postErrorByPost[postId] = res.message.asString(getApplication())
                }
            }
        }
    }

    fun openCommentsBottomSheet(postId: Long) {
        navigateToDestination(
            ThreadCommentsBottomSheetScreenDestination(postId),
            viewModelScope
        )
    }

    private fun targetUsername(): String = requestedUsername ?: currentUsername

    fun navigateUp() {
        navigateUp(viewModelScope)
    }
}
