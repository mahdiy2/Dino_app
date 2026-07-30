package com.dino.message.threadfeature.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.data.spref.DataStoreUtil
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.threadfeature.domain.model.Post
import com.dino.message.threadfeature.domain.model.PostMedia
import com.dino.message.threadfeature.domain.model.VoteTargetType
import com.dino.message.threadfeature.domain.usecase.AddTrackToPlaylistUseCase
import com.dino.message.threadfeature.domain.usecase.GetPostUseCase
import com.dino.message.threadfeature.domain.usecase.VoteUseCase
import com.ramcosta.composedestinations.generated.destinations.ProfileScreenDestination
import com.ramcosta.composedestinations.generated.destinations.CreatePostOptionsBottomSheetScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SharePostBottomSheetScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SharedPostDetailsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.TrackPlaylistAddersDialogScreenDestination
import com.ramcosta.composedestinations.generated.destinations.ThreadCommentsBottomSheetScreenDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SharedPostDetailsViewModel constructor(
    app: Context,
    private val dataStoreUtil: DataStoreUtil,
    private val getPostUseCase: GetPostUseCase,
    private val voteUseCase: VoteUseCase,
    private val addTrackToPlaylistUseCase: AddTrackToPlaylistUseCase,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    private val postId: Long = savedStateHandle.get<Long>("postId") ?: 0L

    private val _post = MutableStateFlow<Post?>(null)
    val post: StateFlow<Post?> = _post.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedVote = MutableStateFlow(0L)
    val selectedVote: StateFlow<Long> = _selectedVote.asStateFlow()
    private val _viewerHasBadge = MutableStateFlow(false)
    val viewerHasBadge: StateFlow<Boolean> = _viewerHasBadge.asStateFlow()
    private val _playlistFeedback = MutableStateFlow<String?>(null)
    val playlistFeedback: StateFlow<String?> = _playlistFeedback.asStateFlow()

    init {
        observeViewerBadgeAccess()
        refresh()
    }

    private fun observeViewerBadgeAccess() = viewModelScope.launch {
        dataStoreUtil.getBadgeAccessEnabled().collect { hasBadge ->
            _viewerHasBadge.value = hasBadge
        }
    }

    fun refresh() {
        if (postId <= 0L) {
            _error.value = "Invalid post id"
            return
        }

        viewModelScope.launch {
            getPostUseCase.execute(postId).collect { res ->
                when (res) {
                    is Resource.Loading -> {
                        _isLoading.value = true
                        _error.value = null
                    }
                    is Resource.Success -> {
                        _isLoading.value = false
                        _post.value = res.data
                        _selectedVote.value = res.data.viewerVote
                        _error.value = null
                    }
                    is Resource.Error -> {
                        _isLoading.value = false
                        _error.value = res.message.asString(getApplication())
                    }
                }
            }
        }
    }

    fun votePost(value: Long) {
        val target = _post.value ?: return
        viewModelScope.launch {
            voteUseCase.execute(
                targetType = VoteTargetType.POST,
                targetId = target.id,
                value = value
            ).collect { res ->
                when (res) {
                    is Resource.Loading -> Unit
                    is Resource.Success -> {
                        _selectedVote.value = value
                        _error.value = null
                    }
                    is Resource.Error -> {
                        _error.value = res.message.asString(getApplication())
                    }
                }
            }
        }
    }

    fun openCommentsBottomSheet() {
        navigateToDestination(
            ThreadCommentsBottomSheetScreenDestination(postId = postId),
            viewModelScope
        )
    }

    fun openShareBottomSheet() {
        val postData = _post.value ?: return
        navigateToDestination(
            SharePostBottomSheetScreenDestination(
                postId = postData.id,
                authorUsername = postData.author.username,
                postText = postData.text.take(240)
            ),
            viewModelScope
        )
    }

    fun openQuotePostOptions() {
        val postData = _post.value ?: return
        navigateToDestination(
            CreatePostOptionsBottomSheetScreenDestination(
                quotedPostId = postData.id
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

    fun openUserProfile(username: String) {
        if (username.isBlank()) return
        navigateToDestination(ProfileScreenDestination(username = username), viewModelScope)
    }

    fun navigateBack() {
        navigateUp(viewModelScope)
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
            TrackPlaylistAddersDialogScreenDestination(
                trackTitle = trackTitle,
                playlistAddersJson = Json.encodeToString(media.playlistAdders)
            ),
            viewModelScope
        )
    }

    fun clearPlaylistFeedback() {
        _playlistFeedback.value = null
    }
}
