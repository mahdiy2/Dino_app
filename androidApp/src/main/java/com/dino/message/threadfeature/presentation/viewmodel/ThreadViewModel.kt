package com.dino.message.threadfeature.presentation.viewmodel

import com.dino.message.corefeature.presentation.util.asString
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.data.socket.AppSocketManager
import com.dino.message.corefeature.data.spref.DataStoreUtil
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.profilefeature.domain.model.Profile
import com.dino.message.profilefeature.domain.usecase.FollowUserUseCase
import com.dino.message.profilefeature.domain.usecase.GetMyProfileUseCase
import com.dino.message.threadfeature.domain.model.FeedMode
import com.dino.message.threadfeature.domain.model.Post
import com.dino.message.threadfeature.domain.model.CreatePostMedia
import com.dino.message.threadfeature.domain.model.PostMedia
import com.dino.message.threadfeature.domain.model.StoryBundle
import com.dino.message.threadfeature.domain.model.StoryDetail
import com.dino.message.threadfeature.domain.model.StoryUserFeedItem
import com.dino.message.threadfeature.domain.model.StoryViewer
import com.dino.message.threadfeature.domain.model.VoteTargetType
import com.dino.message.threadfeature.domain.usecase.AddTrackToPlaylistUseCase
import com.dino.message.threadfeature.domain.usecase.BlockUserUseCase
import com.dino.message.threadfeature.domain.usecase.GetPostsUseCase
import com.dino.message.threadfeature.domain.usecase.IsTrackInPlaylistUseCase
import com.dino.message.threadfeature.domain.usecase.GetStoryBundleUseCase
import com.dino.message.threadfeature.domain.usecase.DeleteStoryUseCase
import com.dino.message.threadfeature.domain.usecase.GetStoryFeedUseCase
import com.dino.message.threadfeature.domain.usecase.GetStoryMentionCandidatesUseCase
import com.dino.message.threadfeature.domain.usecase.GetStoryViewersUseCase
import com.dino.message.threadfeature.domain.usecase.RecordStoryViewUseCase
import com.dino.message.threadfeature.domain.usecase.ReplyToStoryUseCase
import com.dino.message.threadfeature.domain.usecase.SetStoryLikeUseCase
import com.dino.message.threadfeature.domain.usecase.UploadStoryUseCase
import com.dino.message.threadfeature.domain.usecase.VoteUseCase
import com.dino.message.activitynotificationfeature.domain.usecase.GetUnreadCountUseCase
import com.dino.message.threadfeature.presentation.screen.CreatePostOptionsBottomSheetScreen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.dino.message.corefeature.presentation.navigation.AppRoute

data class FollowSuggestion(
    val userId: Long,
    val username: String,
    val name: String?,
    val badge: String?,
    val profileImageUrl: String?,
    val profileImageShape: String?,
)

class ThreadViewModel constructor(
    app: Context,
    private val getPostsUseCase: GetPostsUseCase,
    private val voteUseCase: VoteUseCase,
    private val blockUserUseCase: BlockUserUseCase,
    private val addTrackToPlaylistUseCase: AddTrackToPlaylistUseCase,
    private val isTrackInPlaylistUseCase: IsTrackInPlaylistUseCase,
    private val getStoryFeedUseCase: GetStoryFeedUseCase,
    private val getStoryMentionCandidatesUseCase: GetStoryMentionCandidatesUseCase,
    private val getStoryBundleUseCase: GetStoryBundleUseCase,
    private val deleteStoryUseCase: DeleteStoryUseCase,
    private val recordStoryViewUseCase: RecordStoryViewUseCase,
    private val setStoryLikeUseCase: SetStoryLikeUseCase,
    private val getStoryViewersUseCase: GetStoryViewersUseCase,
    private val replyToStoryUseCase: ReplyToStoryUseCase,
    private val uploadStoryUseCase: UploadStoryUseCase,
    private val followUserUseCase: FollowUserUseCase,
    private val getMyProfileUseCase: GetMyProfileUseCase,
    private val dataStoreUtil: DataStoreUtil,
    private val sPrefManager: SPrefManager,
    private val getUnreadCountUseCase: GetUnreadCountUseCase,
    private val appSocketManager: AppSocketManager,
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    val profile = mutableStateOf<Profile?>(null)
    private val postsPageLimit = 10
    private var nextFeedCursor: String? = null
    private var hasMorePosts = true
    private var isPostsRequestRunning = false
    private var pendingFeedRefresh = false

    private val _posts = mutableStateListOf<Post>()
    val posts: List<Post> = _posts
    private val _selectedFeedMode = MutableStateFlow(FeedMode.FOLLOWERS)
    val selectedFeedMode: StateFlow<FeedMode> = _selectedFeedMode.asStateFlow()
    private val _followingSuggestions = MutableStateFlow<List<FollowSuggestion>>(emptyList())
    val followingSuggestions: StateFlow<List<FollowSuggestion>> = _followingSuggestions.asStateFlow()
    private val _followedSuggestedUsernames = MutableStateFlow<Set<String>>(emptySet())
    val followedSuggestedUsernames: StateFlow<Set<String>> = _followedSuggestedUsernames.asStateFlow()
    private val _isFollowingSuggestionsLoading = MutableStateFlow(false)
    val isFollowingSuggestionsLoading: StateFlow<Boolean> = _isFollowingSuggestionsLoading.asStateFlow()

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
    private val _viewerHasBadge = MutableStateFlow(false)
    val viewerHasBadge: StateFlow<Boolean> = _viewerHasBadge.asStateFlow()
    private val _isBadgeFeaturesDialogDismissed = MutableStateFlow(false)
    val isBadgeFeaturesDialogDismissed: StateFlow<Boolean> =
        _isBadgeFeaturesDialogDismissed.asStateFlow()

    private val postVoteByPost = mutableStateMapOf<Long, Long>()
    private val postErrorByPost = mutableStateMapOf<Long, String?>()

    private val _unreadNotificationCount = MutableStateFlow(0)
    val unreadNotificationCount: StateFlow<Int> = _unreadNotificationCount.asStateFlow()

    private val _blockFeedback = MutableStateFlow<String?>(null)
    val blockFeedback: StateFlow<String?> = _blockFeedback.asStateFlow()

    private val _playlistFeedback = MutableStateFlow<String?>(null)
    val playlistFeedback: StateFlow<String?> = _playlistFeedback.asStateFlow()
    private val _storyFeed = MutableStateFlow<List<StoryUserFeedItem>>(emptyList())
    val storyFeed: StateFlow<List<StoryUserFeedItem>> = _storyFeed.asStateFlow()
    private val _storyFeedError = MutableStateFlow<String?>(null)
    val storyFeedError: StateFlow<String?> = _storyFeedError.asStateFlow()
    private val _isStoryFeedLoading = MutableStateFlow(false)
    val isStoryFeedLoading: StateFlow<Boolean> = _isStoryFeedLoading.asStateFlow()
    private val _activeStoryBundle = MutableStateFlow<StoryBundle?>(null)
    val activeStoryBundle: StateFlow<StoryBundle?> = _activeStoryBundle.asStateFlow()
    private val _isStoryViewerVisible = MutableStateFlow(false)
    val isStoryViewerVisible: StateFlow<Boolean> = _isStoryViewerVisible.asStateFlow()
    private val _storyActionInFlight = MutableStateFlow(false)
    val storyActionInFlight: StateFlow<Boolean> = _storyActionInFlight.asStateFlow()
    private val _storyFeedback = MutableStateFlow<String?>(null)
    val storyFeedback: StateFlow<String?> = _storyFeedback.asStateFlow()
    private val _storyViewers = MutableStateFlow<List<StoryViewer>>(emptyList())
    val storyViewers: StateFlow<List<StoryViewer>> = _storyViewers.asStateFlow()
    private val _storyViewerInsightsVisible = MutableStateFlow(false)
    val storyViewerInsightsVisible: StateFlow<Boolean> = _storyViewerInsightsVisible.asStateFlow()
    private val _storyViewersLoading = MutableStateFlow(false)
    val storyViewersLoading: StateFlow<Boolean> = _storyViewersLoading.asStateFlow()
    private val _storyMentionCandidates = MutableStateFlow<List<String>>(emptyList())
    val storyMentionCandidates: StateFlow<List<String>> = _storyMentionCandidates.asStateFlow()

    private val socketOwner = "ThreadViewModel.activity"
    private var storyMentionSearchJob: Job? = null
    private val currentUsername: String = sPrefManager.getUsername().orEmpty()
    val viewerUsername: String
        get() = currentUsername

    init {
        refresh()
        fetchUnreadCount()
        connectActivitySocket()
        observeViewerBadgeAccess()
        observeBadgeFeaturesDialogState()
    }

    private fun observeViewerBadgeAccess() = viewModelScope.launch {
        dataStoreUtil.getBadgeAccessEnabled().collect { hasBadge ->
            _viewerHasBadge.value = hasBadge
        }
    }

    private fun observeBadgeFeaturesDialogState() = viewModelScope.launch {
        dataStoreUtil.getBadgeFeaturesDialogDismissed().collect { dismissed ->
            _isBadgeFeaturesDialogDismissed.value = dismissed
        }
    }

    fun dismissBadgeFeaturesDialog() = viewModelScope.launch {
        dataStoreUtil.setBadgeFeaturesDialogDismissed(true)
    }

    fun refreshAfterPostIfNeeded() {
        if (sPrefManager.consumeThreadRefreshAfterPost()) {
            refresh()
        }
    }

    fun refresh() {
        if (isPostsRequestRunning) return
        resetFeedState()
        _activeStoryBundle.value = null
        _isStoryViewerVisible.value = false
        _storyViewerInsightsVisible.value = false
        _storyViewers.value = emptyList()
        requestPage(isRefresh = true)
        loadStoryFeed()
        loadProfile()
    }

    fun onFeedModeSelected(mode: FeedMode) {
        if (_selectedFeedMode.value == mode) return
        _selectedFeedMode.value = mode
        clearFollowingSuggestions()
        if (isPostsRequestRunning) {
            pendingFeedRefresh = true
            return
        }
        refreshFeedOnly()
    }

    fun loadNextPage() {
        if (isPostsRequestRunning) return
        if (!hasMorePosts) return
        requestPage(isRefresh = false)
    }

    private fun refreshFeedOnly() {
        if (isPostsRequestRunning) return
        resetFeedState()
        requestPage(isRefresh = true)
    }

    private fun resetFeedState() {
        nextFeedCursor = null
        hasMorePosts = true
        _posts.clear()
        postVoteByPost.clear()
        postErrorByPost.clear()
        clearFollowingSuggestions()
    }

    fun followSuggestedUser(username: String) = viewModelScope.launch {
        if (username.isBlank()) return@launch
        if (_followedSuggestedUsernames.value.contains(username)) return@launch

        followUserUseCase.execute(username).collect { res ->
            when (res) {
                is Resource.Success -> {
                    _followedSuggestedUsernames.value =
                        _followedSuggestedUsernames.value + username
                }
                is Resource.Loading -> Unit
                is Resource.Error -> {
                    _error.value = res.message.asString(getApplication())
                }
            }
        }
    }

    fun confirmFollowSuggestions() {
        if (_followedSuggestedUsernames.value.isEmpty()) return
        refreshFeedOnly()
    }

    private fun clearFollowingSuggestions() {
        _followingSuggestions.value = emptyList()
        _followedSuggestedUsernames.value = emptySet()
        _isFollowingSuggestionsLoading.value = false
    }

    private fun loadFollowingSuggestionsIfNeeded() = viewModelScope.launch {
        if (_selectedFeedMode.value != FeedMode.FOLLOWERS) return@launch
        if (_posts.isNotEmpty()) return@launch
        if (_isFollowingSuggestionsLoading.value) return@launch

        _isFollowingSuggestionsLoading.value = true
        getPostsUseCase.execute(
            mode = FeedMode.MIXED,
            cursor = null,
            limit = 50
        ).collect { res ->
            when (res) {
                is Resource.Success -> {
                    val frequencyByUsername = linkedMapOf<String, Pair<FollowSuggestion, Int>>()
                    res.data.posts.forEach { post ->
                        val author = post.author
                        if (author.username == currentUsername) return@forEach
                        val key = author.username
                        val existing = frequencyByUsername[key]
                        val suggestion = FollowSuggestion(
                            userId = author.id,
                            username = author.username,
                            name = author.name,
                            badge = author.badge,
                            profileImageUrl = author.profile_image_url,
                            profileImageShape = author.profile_image_shape
                        )
                        if (existing == null) {
                            frequencyByUsername[key] = suggestion to 1
                        } else {
                            frequencyByUsername[key] = existing.first to (existing.second + 1)
                        }
                    }

                    _followingSuggestions.value = frequencyByUsername.values
                        .sortedWith(
                            compareByDescending<Pair<FollowSuggestion, Int>> { it.second }
                                .thenByDescending { it.first.userId }
                        )
                        .map { it.first }
                        .take(10)
                    _isFollowingSuggestionsLoading.value = false
                }
                is Resource.Loading -> Unit
                is Resource.Error -> {
                    _followingSuggestions.value = emptyList()
                    _isFollowingSuggestionsLoading.value = false
                }
            }
        }
    }

    private fun loadProfile() = viewModelScope.launch {
        val call = getMyProfileUseCase.execute()


        call.collect { res ->
            when (res) {
                is Resource.Success -> {
                    profile.value = res.data
                    dataStoreUtil.saveBadgeAccessCache(
                        hasBadge = !res.data.badge.isNullOrBlank()
                    )
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

        val cursor = if (isRefresh) null else nextFeedCursor
        getPostsUseCase.execute(
            mode = _selectedFeedMode.value,
            cursor = cursor,
            limit = postsPageLimit
        ).collect { res ->
            when (res) {
                is Resource.Success -> {
                    if (isRefresh) _posts.clear()
                    hasMorePosts = res.data.hasMore
                    nextFeedCursor = res.data.nextCursor
                    val existingPostIds = _posts.mapTo(mutableSetOf()) { it.id }
                    val incomingUniquePosts = res.data.posts.filter { post ->
                        existingPostIds.add(post.id)
                    }
                    _posts.addAll(incomingUniquePosts)
                    seedVotesFromPosts(incomingUniquePosts)
                    if (_selectedFeedMode.value == FeedMode.FOLLOWERS && _posts.isEmpty()) {
                        loadFollowingSuggestionsIfNeeded()
                    } else {
                        clearFollowingSuggestions()
                    }
                    _error.value = null
                    showLoading(isRefresh = false, isLoadMore = false)
                    finishPostsRequest()
                }

                is Resource.Loading -> {
                    showLoading(isRefresh = isRefresh, isLoadMore = !isRefresh)
                }

                is Resource.Error -> {
                    _error.value = res.message.asString(getApplication())
                    showLoading(isRefresh = false, isLoadMore = false)
                    finishPostsRequest()
                }
            }
        }
    }

    private fun finishPostsRequest() {
        isPostsRequestRunning = false
        if (pendingFeedRefresh) {
            pendingFeedRefresh = false
            refreshFeedOnly()
        }
    }

    fun openCommentsBottomSheet(postId: Long) {
        navigateToDestination(
            AppRoute.ThreadCommentsSheet(
                postId
            ), viewModelScope)
    }

    fun openReportBottomSheet(postId: Long) {
        navigateToDestination(
            AppRoute.ReportPostSheet(postId),
            viewModelScope
        )
    }

    fun openShareBottomSheet(post: Post) {
        navigateToDestination(
            AppRoute.SharePostSheet(
                postId = post.id,
                authorUsername = post.author.username,
                postText = post.text.take(240)
            ),
            viewModelScope
        )
    }

    fun openQuotePostOptions(post: Post) {
        navigateToDestination(
            AppRoute.CreatePostOptionsSheet(
                quotedPostId = post.id
            ),
            viewModelScope
        )
    }

    fun openQuotedPost(postId: Long) {
        if (postId <= 0L) return
        navigateToDestination(
            AppRoute.SharedPostDetails(postId = postId),
            viewModelScope
        )
    }

    fun openCreatePostScreen() {
        navigateToDestination(
            AppRoute.CreatePost(),
            viewModelScope
        )
    }

    fun openPostMusicScreen() {
        navigateToDestination(
            AppRoute.PostMusic(),
            viewModelScope
        )
    }

    fun showCreateOptionsSheet(){
        navigateToDestination(
            AppRoute.CreatePostOptionsSheet(),
            viewModelScope
        )
    }

    fun openUserProfile(username: String) {
        navigateToDestination(
            AppRoute.Profile(username = username),
            viewModelScope
        )
    }

    fun openUserProfile() {
        navigateToDestination(
            AppRoute.Profile(username = sPrefManager.getUsername()),
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

    fun clearPlaylistFeedback() {
        _playlistFeedback.value = null
    }

    fun openPlaylistBottomSheet() {
        navigateToDestination(
            AppRoute.PlaylistSheet,
            viewModelScope
        )
    }

    fun openPlaylistBottomSheetIfTrackExists(trackUrl: String) = viewModelScope.launch {
        val normalizedUrl = trackUrl.trim()
        if (normalizedUrl.isBlank()) return@launch

        isTrackInPlaylistUseCase.execute(trackUrl = normalizedUrl).collect { res ->
            if (res is Resource.Success) {
                if (res.data) {
                    openPlaylistBottomSheet()
                }
            }
        }
    }

    fun shouldShowBlockForAuthor(username: String): Boolean {
        return username.isNotBlank() && username != currentUsername
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

    fun openTrackPlaylistAddersDialog(media: PostMedia, trackTitle: String) {
        if (media.playlistAdders.isEmpty()) return
        navigateToDestination(
            AppRoute.TrackPlaylistAddersDialog(
                trackTitle = trackTitle,
                playlistAddersJson = Json.encodeToString(media.playlistAdders)
            ),
            viewModelScope
        )
    }

    fun loadStoryFeed() = viewModelScope.launch {
        getStoryFeedUseCase.execute().collect { res ->
            when (res) {
                is Resource.Success -> {
                    _storyFeed.value = res.data.user_stories
                    _storyFeedError.value = null
                    _isStoryFeedLoading.value = false
                }

                is Resource.Loading -> {
                    _isStoryFeedLoading.value = true
                }

                is Resource.Error -> {
                    _storyFeedError.value = res.message.asString(getApplication())
                    _isStoryFeedLoading.value = false
                }
            }
        }
    }

    fun openStoryById(storyId: Long) = viewModelScope.launch {
        if (storyId <= 0L) return@launch
        getStoryBundleUseCase.execute(storyId).collect { res ->
            when (res) {
                is Resource.Success -> {
                    _activeStoryBundle.value = res.data
                    _isStoryViewerVisible.value = true
                    _storyFeedback.value = null
                    recordStoryView(res.data.story.story_id)
                }

                is Resource.Loading -> {
                    _storyActionInFlight.value = true
                }

                is Resource.Error -> {
                    _storyActionInFlight.value = false
                    _storyFeedback.value = res.message.asString(getApplication())
                }
            }
        }
        _storyActionInFlight.value = false
    }

    fun openStoryForUser(item: StoryUserFeedItem) {
        val firstStoryId = item.story_ids.firstOrNull() ?: return
        openStoryById(firstStoryId)
    }

    fun closeStoryViewer() {
        _isStoryViewerVisible.value = false
        _activeStoryBundle.value = null
        _storyViewerInsightsVisible.value = false
        _storyViewers.value = emptyList()
        _storyActionInFlight.value = false
    }

    fun deleteStory(storyId: Long) = viewModelScope.launch {
        if (storyId <= 0L) return@launch

        deleteStoryUseCase.execute(storyId).collect { res ->
            when (res) {
                is Resource.Success -> {
                    _storyActionInFlight.value = false
                    _storyViewerInsightsVisible.value = false
                    _storyViewers.value = emptyList()
                    _storyFeedback.value = res.data.message
                    _activeStoryBundle.value?.let { current ->
                        val remainingStories = current.user_stories.filterNot { it.story_id == storyId }
                        if (remainingStories.isEmpty()) {
                            openNextStoryBundleOrClose()
                        } else {
                            _activeStoryBundle.value = current.copy(
                                story = remainingStories.first(),
                                user_stories = remainingStories
                            )
                        }
                    }
                    loadStoryFeed()
                }

                is Resource.Loading -> {
                    _storyActionInFlight.value = true
                }

                is Resource.Error -> {
                    _storyActionInFlight.value = false
                    _storyFeedback.value = res.message.asString(getApplication())
                }
            }
        }
    }

    fun openNextStoryBundleOrClose() {
        openAdjacentStoryBundleOrClose(step = 1)
    }

    fun openPreviousStoryBundleOrClose() {
        openAdjacentStoryBundleOrClose(step = -1)
    }

    private fun openAdjacentStoryBundleOrClose(step: Int) {
        if (step == 0) return
        val currentBundle = _activeStoryBundle.value ?: run {
            closeStoryViewer()
            return
        }
        val feed = _storyFeed.value
        if (feed.isEmpty()) {
            closeStoryViewer()
            return
        }

        val ownerIndex = feed.indexOfFirst { it.user_id == currentBundle.owner.user_id }
        if (ownerIndex == -1) {
            closeStoryViewer()
            return
        }

        val targetStoryId = if (step > 0) {
            feed
                .asSequence()
                .drop(ownerIndex + 1)
                .mapNotNull { item -> item.story_ids.firstOrNull() }
                .firstOrNull()
        } else {
            feed
                .asSequence()
                .take(ownerIndex)
                .toList()
                .asReversed()
                .mapNotNull { item -> item.story_ids.lastOrNull() }
                .firstOrNull()
        }

        if (targetStoryId == null) {
            closeStoryViewer()
            return
        }
        openStoryById(targetStoryId)
    }

    fun recordStoryView(storyId: Long) = viewModelScope.launch {
        if (storyId <= 0L) return@launch
        recordStoryViewUseCase.execute(storyId).collect { res ->
            when (res) {
                is Resource.Success -> {
                    _storyActionInFlight.value = false
                    updateActiveStory(storyId) { current ->
                        current.copy(
                            view_count = res.data.view_count,
                            viewer_has_viewed = true
                        )
                    }
                }

                is Resource.Loading -> {
                    _storyActionInFlight.value = true
                }

                is Resource.Error -> {
                    _storyActionInFlight.value = false
                }
            }
        }
    }

    fun setStoryLike(storyId: Long, liked: Boolean) = viewModelScope.launch {
        if (storyId <= 0L) return@launch
        setStoryLikeUseCase.execute(storyId, liked).collect { res ->
            when (res) {
                is Resource.Success -> {
                    _storyActionInFlight.value = false
                    updateActiveStory(storyId) { current ->
                        current.copy(
                            like_count = res.data.like_count,
                            view_count = res.data.view_count,
                            viewer_liked = res.data.liked
                        )
                    }
                    loadStoryFeed()
                }

                is Resource.Loading -> {
                    _storyActionInFlight.value = true
                }

                is Resource.Error -> {
                    _storyActionInFlight.value = false
                    _storyFeedback.value = res.message.asString(getApplication())
                }
            }
        }
    }

    fun replyToActiveStory(storyId: Long, replyText: String) = viewModelScope.launch {
        val trimmed = replyText.trim()
        if (storyId <= 0L || trimmed.isEmpty()) return@launch
        replyToStoryUseCase.execute(storyId, trimmed).collect { res ->
            when (res) {
                is Resource.Success -> {
                    _storyActionInFlight.value = false
                    _storyFeedback.value = res.data.message
                }

                is Resource.Loading -> {
                    _storyActionInFlight.value = true
                }

                is Resource.Error -> {
                    _storyActionInFlight.value = false
                    _storyFeedback.value = res.message.asString(getApplication())
                }
            }
        }
    }

    fun fetchStoryViewers(storyId: Long) = viewModelScope.launch {
        if (storyId <= 0L) return@launch
        getStoryViewersUseCase.execute(storyId, page = 1, limit = 40).collect { res ->
            when (res) {
                is Resource.Success -> {
                    _storyViewers.value = res.data.viewers
                    _storyViewerInsightsVisible.value = true
                    _storyViewersLoading.value = false
                }

                is Resource.Loading -> {
                    _storyViewersLoading.value = true
                }

                is Resource.Error -> {
                    _storyViewersLoading.value = false
                    _storyFeedback.value = res.message.asString(getApplication())
                }
            }
        }
    }

    fun dismissStoryInsights() {
        _storyViewerInsightsVisible.value = false
    }

    fun uploadStory(media: CreatePostMedia, mentions: List<String> = emptyList()) = viewModelScope.launch {
        uploadStoryUseCase.execute(media, mentions).collect { res ->
            when (res) {
                is Resource.Success -> {
                    _storyActionInFlight.value = false
                    _storyFeedback.value = "Story uploaded"
                    loadStoryFeed()
                }

                is Resource.Loading -> {
                    _storyActionInFlight.value = true
                }

                is Resource.Error -> {
                    _storyActionInFlight.value = false
                    _storyFeedback.value = res.message.asString(getApplication())
                }
            }
        }
    }

    fun clearStoryFeedback() {
        _storyFeedback.value = null
    }

    fun requestStoryMentionCandidates(query: String) {
        val normalizedQuery = query.trim().removePrefix("@")
        storyMentionSearchJob?.cancel()
        storyMentionSearchJob = viewModelScope.launch {
            if (normalizedQuery.isNotEmpty()) {
                delay(180)
            }
            getStoryMentionCandidatesUseCase.execute(
                query = normalizedQuery,
                limit = 6
            ).collect { res ->
                when (res) {
                    is Resource.Success -> {
                        _storyMentionCandidates.value = res.data
                            .asSequence()
                            .map { it.trim() }
                            .filter { it.isNotBlank() && !it.equals(currentUsername, ignoreCase = true) }
                            .distinct()
                            .take(6)
                            .toList()
                    }

                    is Resource.Loading -> Unit

                    is Resource.Error -> {
                        _storyMentionCandidates.value = emptyList()
                    }
                }
            }
        }
    }

    private fun updateActiveStory(storyId: Long, transform: (StoryDetail) -> StoryDetail) {
        val current = _activeStoryBundle.value ?: return
        val updatedStories = current.user_stories.map { story ->
            if (story.story_id == storyId) transform(story) else story
        }
        val updatedCurrentStory = updatedStories.firstOrNull { it.story_id == storyId } ?: current.story
        _activeStoryBundle.value = current.copy(
            story = updatedCurrentStory,
            user_stories = updatedStories
        )
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
        navigateToDestination(
            route = AppRoute.Main,
            scope = viewModelScope
        )
    }

    fun openSearchScreen() {
        navigateToDestination(AppRoute.Search, viewModelScope)
    }

    fun openActivityNotifications() {
        navigateToDestination(AppRoute.ActivityNotification, viewModelScope)
    }

    fun openChatWithUser(username: String) {
        val normalized = username.trim()
        if (normalized.isBlank()) return
        navigateToDestination(
            AppRoute.Chat(chatId = normalized),
            viewModelScope
        )
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
        appSocketManager.registerListener(socketOwner, "activity_notification") {
            _unreadNotificationCount.value = _unreadNotificationCount.value + 1
        }
        appSocketManager.connect()
    }

    override fun onCleared() {
        storyMentionSearchJob?.cancel()
        appSocketManager.unregisterOwner(socketOwner)
        super.onCleared()
    }
}
