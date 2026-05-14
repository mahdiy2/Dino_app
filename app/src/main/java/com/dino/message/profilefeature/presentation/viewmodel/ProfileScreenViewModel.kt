package com.dino.message.profilefeature.presentation.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.domain.usecase.ClearLocalSessionUseCase
import com.dino.message.corefeature.data.spref.DataStoreUtil
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
import com.dino.message.threadfeature.domain.model.PostMedia
import com.dino.message.threadfeature.domain.model.StoryUserFeedItem
import com.dino.message.threadfeature.domain.model.VoteTargetType
import com.dino.message.threadfeature.domain.usecase.AddTrackToPlaylistUseCase
import com.dino.message.threadfeature.domain.usecase.GetStoryFeedUseCase
import com.dino.message.threadfeature.domain.usecase.VoteUseCase
import com.ramcosta.composedestinations.generated.destinations.CreatePostScreenDestination
import com.ramcosta.composedestinations.generated.destinations.CreatePostOptionsBottomSheetScreenDestination
import com.ramcosta.composedestinations.generated.destinations.DeletePostBottomSheetScreenDestination
import com.ramcosta.composedestinations.generated.destinations.EditProfileScreenDestination
import com.ramcosta.composedestinations.generated.destinations.LoginScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ProfileFollowListScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ReferralScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PostMusicScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ProfileSettingsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ReportPostBottomSheetScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SharePostBottomSheetScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SharedPostDetailsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.TrackPlaylistAddersDialogScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ThreadCommentsBottomSheetScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ThreadScreenDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class ProfileScreenViewModel @Inject constructor(
    @ApplicationContext app: Context,
    private val dataStoreUtil: DataStoreUtil,
    private val sPrefManager: SPrefManager,
    private val getMyProfileUseCase: GetMyProfileUseCase,
    private val getProfileUseCase: GetProfileUseCase,
    private val getProfilePostsUseCase: GetProfilePostsUseCase,
    private val getFollowStatusUseCase: GetFollowStatusUseCase,
    private val followUserUseCase: FollowUserUseCase,
    private val unfollowUserUseCase: UnfollowUserUseCase,
    private val voteUseCase: VoteUseCase,
    private val addTrackToPlaylistUseCase: AddTrackToPlaylistUseCase,
    private val getStoryFeedUseCase: GetStoryFeedUseCase,
    private val clearLocalSessionUseCase: ClearLocalSessionUseCase,
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {
    val profile = mutableStateOf<Profile?>(null)
    val posts = mutableStateListOf<Post>()
    private val postsPageLimit = 20
    private var currentPostsPage = 1
    private var totalPosts = Int.MAX_VALUE
    private var isPostsRequestRunning = false

    private val _isProfileLoading = MutableStateFlow(false)
    val isProfileLoading: StateFlow<Boolean> = _isProfileLoading.asStateFlow()

    private val _isPostsLoading = MutableStateFlow(false)
    val isPostsLoading: StateFlow<Boolean> = _isPostsLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMorePosts = MutableStateFlow(true)
    val hasMorePosts: StateFlow<Boolean> = _hasMorePosts.asStateFlow()

    private val _isActionLoading = MutableStateFlow(false)
    val isActionLoading: StateFlow<Boolean> = _isActionLoading.asStateFlow()

    private val _isOwnProfile = MutableStateFlow(true)
    val isOwnProfile: StateFlow<Boolean> = _isOwnProfile.asStateFlow()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _playlistFeedback = MutableStateFlow<String?>(null)
    val playlistFeedback: StateFlow<String?> = _playlistFeedback.asStateFlow()
    private val _viewerHasBadge = MutableStateFlow(false)
    val viewerHasBadge: StateFlow<Boolean> = _viewerHasBadge.asStateFlow()
    private val _profileStoryItem = MutableStateFlow<StoryUserFeedItem?>(null)
    val profileStoryItem: StateFlow<StoryUserFeedItem?> = _profileStoryItem.asStateFlow()

    private val currentUsername = sPrefManager.getUsername().toString().trim()
    private var requestedUsername: String? = null
    private var initializedKey: String? = null
    private var shouldRefreshWhenScreenResumes = false

    private val postVoteByPost = mutableStateMapOf<Long, Long>()
    private val postErrorByPost = mutableStateMapOf<Long, String?>()

    init {
        observeViewerBadgeAccess()
    }

    fun initialize(profileUsername: String?) {
        val key = profileUsername?.trim()?.ifEmpty { null }
        if (initializedKey == key) return
        initializedKey = key

        requestedUsername = key
        _isOwnProfile.value = key == null || key.equals(currentUsername, ignoreCase = true)
        _isFollowing.value = false
        _profileStoryItem.value = null
        refresh()
    }

    fun refresh() {
        if (isPostsRequestRunning) return
        currentPostsPage = 1
        totalPosts = Int.MAX_VALUE
        posts.clear()
        _isPostsLoading.value = false
        _isLoadingMore.value = false
        _hasMorePosts.value = true
        postVoteByPost.clear()
        postErrorByPost.clear()
        loadProfile()
        loadPosts()
        loadFollowStatus()
        loadProfileStoryState()
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
                    if (_isOwnProfile.value) {
                        dataStoreUtil.saveBadgeAccessCache(
                            hasBadge = !res.data.badge.isNullOrBlank()
                        )
                    }
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

    private fun loadPosts() {
        requestPostsPage(isRefresh = true)
    }

    fun loadNextPosts() {
        if (isPostsRequestRunning || !_hasMorePosts.value) return
        requestPostsPage(isRefresh = false)
    }

    private fun requestPostsPage(isRefresh: Boolean) = viewModelScope.launch {
        if (isPostsRequestRunning) return@launch
        isPostsRequestRunning = true

        getProfilePostsUseCase.execute(
            username = targetUsername(),
            page = currentPostsPage,
            limit = postsPageLimit
        ).collect { res ->
            when (res) {
                is Resource.Success -> {
                    if (isRefresh) {
                        posts.clear()
                    }

                    totalPosts = res.data.total
                    val existingPostIds = posts.mapTo(mutableSetOf()) { it.id }
                    val incomingUniquePosts = res.data.posts.filter { post ->
                        existingPostIds.add(post.id)
                    }
                    posts.addAll(incomingUniquePosts)
                    seedVotesFromPosts(incomingUniquePosts)

                    if (res.data.posts.isNotEmpty()) {
                        currentPostsPage = res.data.page + 1
                    }
                    _hasMorePosts.value =
                        posts.size < totalPosts && incomingUniquePosts.isNotEmpty()
                    _error.value = null
                    _isPostsLoading.value = false
                    _isLoadingMore.value = false
                    isPostsRequestRunning = false
                }

                is Resource.Loading -> {
                    _isPostsLoading.value = isRefresh
                    _isLoadingMore.value = !isRefresh
                }

                is Resource.Error -> {
                    _isPostsLoading.value = false
                    _isLoadingMore.value = false
                    _error.value = res.message.asString(getApplication())
                    isPostsRequestRunning = false
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

    private fun observeViewerBadgeAccess() = viewModelScope.launch {
        dataStoreUtil.getBadgeAccessEnabled().collect { hasBadge ->
            _viewerHasBadge.value = hasBadge
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
                        loadProfileStoryState()
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

    fun openReferralScreen() {
        if (!_isOwnProfile.value || _isActionLoading.value) return
        navigateToDestination(ReferralScreenDestination, viewModelScope)
    }

    fun openProfileSettingsScreen() {
        if (!_isOwnProfile.value || _isActionLoading.value) return
        navigateToDestination(ProfileSettingsScreenDestination, viewModelScope)
    }

    fun logout() = viewModelScope.launch {
        clearLocalSessionUseCase.execute()
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

    private fun seedVotesFromPosts(posts: List<Post>) {
        posts.forEach { post ->
            postVoteByPost[post.id] = post.viewerVote
        }
    }

    fun openCommentsBottomSheet(postId: Long) {
        navigateToDestination(
            ThreadCommentsBottomSheetScreenDestination(postId),
            viewModelScope
        )
    }

    fun openReportBottomSheet(postId: Long) {
        navigateToDestination(
            ReportPostBottomSheetScreenDestination(postId),
            viewModelScope
        )
    }

    fun openShareBottomSheet(post: Post) {
        navigateToDestination(
            SharePostBottomSheetScreenDestination(
                postId = post.id,
                authorUsername = post.author.username,
                postText = post.text.take(240)
            ),
            viewModelScope
        )
    }

    fun openQuotePostOptions(post: Post) {
        shouldRefreshWhenScreenResumes = true
        navigateToDestination(
            CreatePostOptionsBottomSheetScreenDestination(
                quotedPostId = post.id
            ),
            viewModelScope
        )
    }

    fun openQuotedPost(postId: Long) {
        if (postId <= 0L) return
        navigateToDestination(
            SharedPostDetailsScreenDestination(postId = postId),
            viewModelScope
        )
    }

    fun addTrackToPlaylist(mediaId: Long) = viewModelScope.launch {
        if (mediaId <= 0L) return@launch
        addTrackToPlaylistUseCase.execute(mediaId).collect { res ->
            when (res) {
                is Resource.Success -> {
                    _playlistFeedback.value = res.data.message
                }

                is Resource.Loading -> Unit

                is Resource.Error -> {
                    _playlistFeedback.value = res.message.asString(getApplication())
                }
            }
        }
    }

    fun clearPlaylistFeedback() {
        _playlistFeedback.value = null
    }

    fun openTrackPlaylistAddersDialog(media: PostMedia, trackTitle: String) {
        if (media.playlistAdders.isEmpty()) return
        navigateToDestination(
            TrackPlaylistAddersDialogScreenDestination(
                trackTitle = trackTitle,
                playlistAddersJson = Json.encodeToString(media.playlistAdders)
            ),
            viewModelScope
        )
    }

    fun openProfileStory(storyId: Long) {
        if (storyId <= 0L) return
        navigateToDestination(
            ThreadScreenDestination(initialStoryId = storyId),
            viewModelScope
        )
    }

    fun openDeletePostBottomSheet(postId: Long) {
        if (!_isOwnProfile.value) return
        shouldRefreshWhenScreenResumes = true
        navigateToDestination(
            DeletePostBottomSheetScreenDestination(postId),
            viewModelScope
        )
    }

    fun openEditPostScreen(post: Post) {
        if (!_isOwnProfile.value) return
        shouldRefreshWhenScreenResumes = true
        val isMusicPost = post.media.any { it.mime_type.startsWith("audio/", ignoreCase = true) }
        if (isMusicPost) {
            navigateToDestination(
                PostMusicScreenDestination(
                    editPostId = post.id
                ),
                viewModelScope
            )
            return
        }

        navigateToDestination(
            CreatePostScreenDestination(
                editPostId = post.id
            ),
            viewModelScope
        )
    }

    fun showCreateOptionsSheet(){
        shouldRefreshWhenScreenResumes = true
        navigateToDestination(
            CreatePostOptionsBottomSheetScreenDestination(),
            viewModelScope
        )
    }

    private fun loadProfileStoryState() = viewModelScope.launch {
        getStoryFeedUseCase.execute().collect { res ->
            when (res) {
                is Resource.Success -> {
                    val target = targetUsername()
                    _profileStoryItem.value = res.data.user_stories.firstOrNull { item ->
                        item.username.equals(target, ignoreCase = true)
                    }
                }

                is Resource.Loading -> Unit

                is Resource.Error -> {
                    _profileStoryItem.value = null
                }
            }
        }
    }

    private fun targetUsername(): String = requestedUsername ?: currentUsername

    fun navigateUp() {
        navigateUp(viewModelScope)
    }
}
