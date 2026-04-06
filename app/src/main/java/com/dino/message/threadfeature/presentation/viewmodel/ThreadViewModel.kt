package com.dino.message.threadfeature.presentation.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.BuildConfig
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.util.PopBackStackLevel
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.profilefeature.domain.model.Profile
import com.dino.message.profilefeature.domain.usecase.GetMyProfileUseCase
import com.dino.message.threadfeature.domain.model.Post
import com.dino.message.threadfeature.domain.model.VoteTargetType
import com.dino.message.threadfeature.domain.usecase.BlockUserUseCase
import com.dino.message.threadfeature.domain.usecase.GetPostsUseCase
import com.dino.message.threadfeature.domain.usecase.VoteUseCase
import com.dino.message.activitynotificationfeature.domain.usecase.GetUnreadCountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.ramcosta.composedestinations.generated.destinations.CreatePostScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SearchScreenDestination
import com.ramcosta.composedestinations.generated.destinations.MainScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ProfileScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ActivityNotificationScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ReportPostBottomSheetScreenDestination
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.ramcosta.composedestinations.generated.destinations.ThreadCommentsBottomSheetScreenDestination

@HiltViewModel
class ThreadViewModel @Inject constructor(
    @ApplicationContext app: Context,
    private val getPostsUseCase: GetPostsUseCase,
    private val voteUseCase: VoteUseCase,
    private val blockUserUseCase: BlockUserUseCase,
    private val getMyProfileUseCase: GetMyProfileUseCase,
    private val sPrefManager: SPrefManager,
    private val getUnreadCountUseCase: GetUnreadCountUseCase
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    val profile = mutableStateOf<Profile?>(null)
    private val postsPageLimit = 10
    private var currentPostsPage = 1
    private var totalPosts = Int.MAX_VALUE
    private var isPostsRequestRunning = false
    private var shouldRefreshWhenScreenResumes = false

    private val _posts = mutableStateListOf<Post>()
    val posts: List<Post> = _posts

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _profileError = MutableStateFlow<String?>(null)
    val profileError: StateFlow<String?> = _profileError.asStateFlow()

    private val _isProfileLoading = MutableStateFlow(false)
    val isProfileLoading: StateFlow<Boolean> = _isProfileLoading.asStateFlow()

    private val postVoteByPost = mutableStateMapOf<Long, Long>()
    private val postErrorByPost = mutableStateMapOf<Long, String?>()

    private val _unreadNotificationCount = MutableStateFlow(0)
    val unreadNotificationCount: StateFlow<Int> = _unreadNotificationCount.asStateFlow()

    private val _blockFeedback = MutableStateFlow<String?>(null)
    val blockFeedback: StateFlow<String?> = _blockFeedback.asStateFlow()

    private var activitySocket: Socket? = null
    private val currentUsername: String = sPrefManager.getUsername().orEmpty()

    init {
        refresh()
        fetchUnreadCount()
        connectActivitySocket()
    }

    fun refresh() {
        if (isPostsRequestRunning) return
        currentPostsPage = 1
        totalPosts = Int.MAX_VALUE
        _posts.clear()
        postVoteByPost.clear()
        postErrorByPost.clear()
        requestPage(isRefresh = true)
        loadProfile()
    }

    fun loadNextPage() {
        if (isPostsRequestRunning) return
        if (_posts.size >= totalPosts) return
        requestPage(isRefresh = false)
    }

    private fun loadProfile() = viewModelScope.launch {
        val call = getMyProfileUseCase.execute()


        call.collect { res ->
            when (res) {
                is Resource.Success -> {
                    profile.value = res.data
                    _profileError.value = null
                    _isProfileLoading.value = false
                }

                is Resource.Loading -> {
                    _isProfileLoading.value = true
                }

                is Resource.Error -> {
                    _profileError.value = res.message.asString(getApplication())
                    _isProfileLoading.value = false
                }
            }
        }
    }

    private fun requestPage(isRefresh: Boolean) = viewModelScope.launch {
        if (isPostsRequestRunning) return@launch
        isPostsRequestRunning = true

        getPostsUseCase.execute(page = currentPostsPage, limit = postsPageLimit).collect { res ->
            when (res) {
                is Resource.Success -> {
                    if (isRefresh) _posts.clear()
                    totalPosts = res.data.total
                    _posts.addAll(res.data.posts)
                    seedVotesFromPosts(res.data.posts)
                    if (res.data.posts.isNotEmpty()) {
                        currentPostsPage = res.data.page + 1
                    }
                    _error.value = null
                    showLoading(isRefresh = false, isLoadMore = false)
                    isPostsRequestRunning = false
                }

                is Resource.Loading -> {
                    showLoading(isRefresh = isRefresh, isLoadMore = !isRefresh)
                }

                is Resource.Error -> {
                    _error.value = res.message.asString(getApplication())
                    showLoading(isRefresh = false, isLoadMore = false)
                    isPostsRequestRunning = false
                }
            }
        }
    }

    fun openCommentsBottomSheet(postId: Long) {
        navigateToDestination(
            ThreadCommentsBottomSheetScreenDestination(
                postId
            ), viewModelScope)
    }

    fun openReportBottomSheet(postId: Long) {
        navigateToDestination(
            ReportPostBottomSheetScreenDestination(postId),
            viewModelScope
        )
    }

    fun openCreatePostScreen() {
        shouldRefreshWhenScreenResumes = true
        navigateToDestination(
            CreatePostScreenDestination,
            viewModelScope
        )
    }

    fun openUserProfile(username: String) {
        navigateToDestination(
            ProfileScreenDestination(username = username),
            viewModelScope
        )
    }

    fun openUserProfile() {
        navigateToDestination(
            ProfileScreenDestination(username = sPrefManager.getUsername()),
            viewModelScope
        )
    }

    fun refreshIfNeededAfterCreatePost() {
        if (!shouldRefreshWhenScreenResumes) return
        shouldRefreshWhenScreenResumes = false
        refresh()
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

    fun blockUser(username: String) = viewModelScope.launch {
        if (username.isBlank() || username == currentUsername) {
            return@launch
        }

        blockUserUseCase.execute(username).collect { res ->
            when (res) {
                is Resource.Success -> {
                    _blockFeedback.value = res.data.message
                    refresh()
                }

                is Resource.Loading -> Unit

                is Resource.Error -> {
                    _blockFeedback.value = res.message.asString(getApplication())
                }
            }
        }
    }

    fun clearBlockFeedback() {
        _blockFeedback.value = null
    }

    fun shouldShowBlockForAuthor(username: String): Boolean {
        return username.isNotBlank() && username != currentUsername
    }

    private fun seedVotesFromPosts(posts: List<Post>) {
        posts.forEach { post ->
            postVoteByPost[post.id] = post.viewerVote
        }
    }

    private fun showLoading(isRefresh: Boolean, isLoadMore: Boolean) {
        _isRefreshing.value = isRefresh
        _isLoadingMore.value = isLoadMore
    }

    fun navigateChat(){
        navigateWithPopBackStackToDestination(
            direction = MainScreenDestination,
            scope = viewModelScope,
            level = PopBackStackLevel.ALL
        )
    }

    fun openSearchScreen() {
        navigateToDestination(SearchScreenDestination, viewModelScope)
    }

    fun openActivityNotifications() {
        navigateToDestination(ActivityNotificationScreenDestination, viewModelScope)
    }

    fun fetchUnreadCount() = viewModelScope.launch {
        getUnreadCountUseCase.execute().collect { res ->
            when (res) {
                is Resource.Success -> {
                    _unreadNotificationCount.value = res.data.unread_count
                }
                is Resource.Loading -> Unit
                is Resource.Error -> Unit
            }
        }
    }

    private fun connectActivitySocket() {
        val token = sPrefManager.getToken().trim()
        if (token.isBlank()) return

        val cleanToken = token.removePrefix("Bearer ").removePrefix("bearer ").trim()
        val baseUrl = BuildConfig.BASE_URL.trimEnd('/')

        try {
            val opts = IO.Options.builder()
                .setForceNew(false)
                .setMultiplex(true)
                .setReconnection(true)
                .setReconnectionAttempts(Int.MAX_VALUE)
                .setReconnectionDelay(2_000)
                .setReconnectionDelayMax(15_000)
                .setTimeout(20_000)
                .setPath("/socket.io/")
                .setTransports(arrayOf("websocket", "polling"))
                .setAuth(mapOf("token" to cleanToken))
                .build()

            val socket = IO.socket(baseUrl, opts)

            socket.on("activity_notification") {
                _unreadNotificationCount.value = _unreadNotificationCount.value + 1
            }

            activitySocket = socket
            socket.connect()
        } catch (_: Exception) {
        }
    }

    override fun onCleared() {
        activitySocket?.off("activity_notification")
        activitySocket?.disconnect()
        activitySocket = null
        super.onCleared()
    }
}
