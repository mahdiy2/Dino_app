package com.dino.message.threadfeature.presentation.viewmodel

import com.dino.message.corefeature.presentation.util.asString
import android.content.Context
import android.webkit.MimeTypeMap
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.domain.upload.MediaMimePolicy
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.data.upload.PlatformPickedMedia
import com.dino.message.corefeature.data.upload.PlatformMediaReference
import com.dino.message.corefeature.data.upload.PlatformMediaResolver
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.threadfeature.domain.model.CreatePostMedia
import com.dino.message.threadfeature.domain.model.Post
import com.dino.message.threadfeature.domain.model.PostMedia
import com.dino.message.threadfeature.domain.model.QuotedPostPreview
import com.dino.message.threadfeature.domain.usecase.CreatePostUseCase
import com.dino.message.threadfeature.domain.usecase.EditPostUseCase
import com.dino.message.threadfeature.domain.usecase.GetPostUseCase
import com.dino.message.threadfeature.presentation.util.resolveMusicMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SelectedMusicTrack(
    val reference: PlatformMediaReference,
    val displayName: String,
    val title: String,
    val artist: String?,
    val mimeType: String,
    val sizeBytes: Long? = null,
)

class PostMusicViewModel constructor(
    app: Context,
    private val sPrefManager: SPrefManager,
    private val createPostUseCase: CreatePostUseCase,
    private val editPostUseCase: EditPostUseCase,
    private val getPostUseCase: GetPostUseCase,
    private val mediaResolver: PlatformMediaResolver,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    private val quotedPostIdArg: Long = savedStateHandle.get<Long>("quotedPostId") ?: 0L
    private val editPostIdArg: Long = savedStateHandle.get<Long>("editPostId") ?: 0L
    val quotedPostId: Long? = quotedPostIdArg.takeIf { it > 0L }
    val editPostId: Long? = editPostIdArg.takeIf { it > 0L }
    val isEditMode: Boolean = editPostId != null

    val postDraft = mutableStateOf("")
    val followersOnly = mutableStateOf(false)
    val selectedTrack = mutableStateOf<SelectedMusicTrack?>(null)
    val existingTrack = mutableStateOf<PostMedia?>(null)

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()
    private val _isInitialPostLoading = MutableStateFlow(false)
    val isInitialPostLoading: StateFlow<Boolean> = _isInitialPostLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _quotedPost = MutableStateFlow<QuotedPostPreview?>(null)
    val quotedPost: StateFlow<QuotedPostPreview?> = _quotedPost.asStateFlow()
    private val _isQuotedPostLoading = MutableStateFlow(false)
    val isQuotedPostLoading: StateFlow<Boolean> = _isQuotedPostLoading.asStateFlow()
    private val _quotedPostError = MutableStateFlow<String?>(null)
    val quotedPostError: StateFlow<String?> = _quotedPostError.asStateFlow()

    init {
        if (isEditMode) {
            loadPostForEditing()
        } else {
            loadQuotedPostPreviewIfNeeded()
        }
    }

    fun onPostDraftChanged(value: String) {
        postDraft.value = value
        if (_error.value != null) _error.value = null
    }

    fun onFollowersOnlyChanged(value: Boolean) {
        followersOnly.value = value
    }

    fun onMusicPicked(reference: PlatformMediaReference) {
        val pickedMedia = mediaResolver.resolvePickedMedia(reference, displayNameHint = "music_track")
        val displayName = pickedMedia.displayName
        val mimeType = resolveMusicMimeType(pickedMedia)
        if (mimeType == null) {
            _error.value = "Selected file is not a supported music file"
            return
        }

        val metadata = mediaResolver.metadata(reference)
        val resolved = resolveMusicMetadata(
            displayName = displayName,
            title = metadata.title,
            artist = metadata.artist,
            titleFallback = "Unknown track"
        )
        selectedTrack.value = SelectedMusicTrack(
            reference = reference,
            displayName = displayName,
            title = resolved.title,
            artist = resolved.artist,
            mimeType = mimeType,
            sizeBytes = pickedMedia.sizeBytes,
        )
        if (isEditMode) {
            existingTrack.value = null
        }
        _error.value = null
    }

    fun clearSelectedTrack() {
        selectedTrack.value = null
        existingTrack.value = null
    }

    fun submitPost() {
        if (_isSubmitting.value) return

        val track = selectedTrack.value
        val originalTrack = existingTrack.value
        if (track == null && originalTrack == null) {
            _error.value = "Please select one music track"
            return
        }

        val text = postDraft.value.trim()

        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val media = if (track != null) {
                    withContext(Dispatchers.IO) {
                        buildMedia(track)
                    } ?: run {
                        _error.value = "Failed to read selected music file"
                        return@launch
                    }
                } else {
                    null
                }

                val submitFlow = if (isEditMode) {
                    editPostUseCase.execute(
                        postId = editPostId ?: return@launch,
                        text = text,
                        mediaFiles = media?.let { listOf(it) } ?: emptyList(),
                        keepMediaIds = originalTrack?.let { listOf(it.id) } ?: emptyList(),
                        followersOnly = null
                    )
                } else {
                    createPostUseCase.execute(
                        text = text,
                        mediaFiles = listOfNotNull(media),
                        followersOnly = followersOnly.value,
                        quotedPostId = quotedPostId,
                    )
                }

                submitFlow.collect { res ->
                    when (res) {
                        is Resource.Loading -> Unit
                        is Resource.Success -> {
                            _error.value = null
                            sPrefManager.markThreadRefreshAfterPost()
                            postDraft.value = ""
                            followersOnly.value = false
                            selectedTrack.value = null
                            existingTrack.value = null
                            navigateUp(viewModelScope)
                        }
                        is Resource.Error -> {
                            _error.value = res.message.asString(getApplication())
                        }
                    }
                }
            } catch (_: Exception) {
                _error.value = "Failed to upload post. Please try again."
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun closeScreen() {
        navigateUp(viewModelScope)
    }

    private fun loadQuotedPostPreviewIfNeeded() {
        val quotedPostId = quotedPostId ?: return

        viewModelScope.launch {
            getPostUseCase.execute(quotedPostId).collect { res ->
                when (res) {
                    is Resource.Loading -> {
                        _isQuotedPostLoading.value = true
                        _quotedPostError.value = null
                    }

                    is Resource.Success -> {
                        _isQuotedPostLoading.value = false
                        _quotedPostError.value = null
                        _quotedPost.value = res.data.toQuotedPostPreview()
                    }

                    is Resource.Error -> {
                        _isQuotedPostLoading.value = false
                        _quotedPostError.value = res.message.asString(getApplication())
                    }
                }
            }
        }
    }

    private fun loadPostForEditing() {
        val postId = editPostId ?: return

        viewModelScope.launch {
            getPostUseCase.execute(postId).collect { res ->
                when (res) {
                    is Resource.Loading -> {
                        _isInitialPostLoading.value = true
                        _error.value = null
                    }

                    is Resource.Success -> {
                        _isInitialPostLoading.value = false
                        _error.value = null
                        postDraft.value = res.data.text
                        existingTrack.value = res.data.media.firstOrNull {
                            it.mime_type.startsWith("audio/", ignoreCase = true)
                        }
                    }

                    is Resource.Error -> {
                        _isInitialPostLoading.value = false
                        _error.value = res.message.asString(getApplication())
                    }
                }
            }
        }
    }

    private fun buildMedia(track: SelectedMusicTrack): CreatePostMedia? {
        if (!mediaResolver.canRead(track.reference)) return null

        return CreatePostMedia(
            file = mediaResolver.readableMediaFile(
                source = track.reference,
                displayName = track.displayName,
                mimeType = track.mimeType,
                sizeBytes = track.sizeBytes,
            ),
            trackTitle = track.title,
            trackArtist = track.artist
        )
    }

    private fun resolveMusicMimeType(pickedMedia: PlatformPickedMedia): String? {
        val rawResolverMime = MediaMimePolicy.normalizeMimeType(pickedMedia.mimeType)
        if (MediaMimePolicy.isBlockedForSecurity(rawResolverMime)) {
            return null
        }
        if (rawResolverMime.startsWith("audio/")) {
            return rawResolverMime
        }

        val retrieverMime = mediaResolver.metadata(pickedMedia.reference).mimeType.orEmpty()
        if (retrieverMime.startsWith("audio/")) {
            return retrieverMime
        }

        val extension = pickedMedia.displayName
            .substringAfterLast('.', "")
            .lowercase()
            .takeIf { it.isNotBlank() }
        if (extension != null) {
            val guessedMime = MediaMimePolicy.normalizeMimeType(
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            )
            if (guessedMime.startsWith("audio/")) {
                return guessedMime
            }
            val resolverMimeIsGeneric = rawResolverMime.isBlank() ||
                rawResolverMime == "application/octet-stream"
            if (resolverMimeIsGeneric && !MediaMimePolicy.isBlockedForSecurity("audio/$extension")) {
                return "audio/$extension"
            }
        }

        return null
    }

    private fun Post.toQuotedPostPreview(): QuotedPostPreview = QuotedPostPreview(
        id = id,
        text = text,
        author = author,
        created_at = created_at,
        editedAt = editedAt,
        isEdited = isEdited,
        media = media
    )
}
