package com.dino.message.searchfeature.presentation.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.searchfeature.domain.model.SearchTab
import com.dino.message.searchfeature.domain.model.SearchUser
import com.dino.message.searchfeature.domain.usecase.SearchAllUseCase
import com.dino.message.searchfeature.domain.usecase.SearchPostsUseCase
import com.dino.message.searchfeature.domain.usecase.SearchUsersUseCase
import com.dino.message.threadfeature.domain.model.Post
import com.dino.message.threadfeature.domain.model.VoteTargetType
import com.dino.message.threadfeature.domain.usecase.VoteUseCase
import com.ramcosta.composedestinations.generated.destinations.ProfileScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ThreadCommentsBottomSheetScreenDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    @ApplicationContext app: Context,
    private val searchUsersUseCase: SearchUsersUseCase,
    private val searchPostsUseCase: SearchPostsUseCase,
    private val searchAllUseCase: SearchAllUseCase,
    private val voteUseCase: VoteUseCase
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    val query = mutableStateOf("")
    val selectedTab = mutableStateOf(SearchTab.BOTH)

    private val _users = mutableStateListOf<SearchUser>()
    val users: List<SearchUser> = _users

    private val _posts = mutableStateListOf<Post>()
    val posts: List<Post> = _posts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var searchJob: Job? = null
    private val searchLimit = 20

    private val postVoteByPost = mutableStateMapOf<Long, Long>()
    private val postErrorByPost = mutableStateMapOf<Long, String?>()

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
        debounceSearch()
    }

    fun onTabChange(tab: SearchTab) {
        selectedTab.value = tab
        performSearch()
    }

    private fun debounceSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400)
            performSearch()
        }
    }

    fun performSearch() {
        val currentQuery = query.value.trim()
        if (currentQuery.isBlank()) {
            _users.clear()
            _posts.clear()
            _error.value = null
            return
        }

        viewModelScope.launch {
            when (selectedTab.value) {
                SearchTab.USERS -> searchUsers(currentQuery)
                SearchTab.POSTS -> searchPosts(currentQuery)
                SearchTab.BOTH -> searchAll(currentQuery)
            }
        }
    }

    private suspend fun searchUsers(query: String) {
        searchUsersUseCase.execute(query = query, page = 1, limit = searchLimit).collect { res ->
            when (res) {
                is Resource.Loading -> {
                    _isLoading.value = true
                }
                is Resource.Success -> {
                    _users.clear()
                    _users.addAll(res.data.users)
                    _posts.clear()
                    _error.value = null
                    _isLoading.value = false
                }
                is Resource.Error -> {
                    _error.value = res.message.asString(getApplication())
                    _isLoading.value = false
                }
            }
        }
    }

    private suspend fun searchPosts(query: String) {
        searchPostsUseCase.execute(query = query, page = 1, limit = searchLimit).collect { res ->
            when (res) {
                is Resource.Loading -> {
                    _isLoading.value = true
                }
                is Resource.Success -> {
                    _posts.clear()
                    _posts.addAll(res.data.posts)
                    _users.clear()
                    _error.value = null
                    _isLoading.value = false
                }
                is Resource.Error -> {
                    _error.value = res.message.asString(getApplication())
                    _isLoading.value = false
                }
            }
        }
    }

    private suspend fun searchAll(query: String) {
        searchAllUseCase.execute(query = query, page = 1, limit = searchLimit).collect { res ->
            when (res) {
                is Resource.Loading -> {
                    _isLoading.value = true
                }
                is Resource.Success -> {
                    _users.clear()
                    _users.addAll(res.data.users)
                    _posts.clear()
                    _posts.addAll(res.data.posts)
                    _error.value = null
                    _isLoading.value = false
                }
                is Resource.Error -> {
                    _error.value = res.message.asString(getApplication())
                    _isLoading.value = false
                }
            }
        }
    }

    fun openUserProfile(username: String) {
        navigateToDestination(
            ProfileScreenDestination(username = username),
            viewModelScope
        )
    }

    fun navigateBack() {
        navigateUp(viewModelScope)
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
}
