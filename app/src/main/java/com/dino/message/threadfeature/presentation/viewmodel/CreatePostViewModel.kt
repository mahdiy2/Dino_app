package com.dino.message.threadfeature.presentation.viewmodel

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.data.upload.MediaFileResolver
import com.dino.message.corefeature.data.upload.MediaMimePolicy
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.threadfeature.domain.model.CreatePostMedia
import com.dino.message.threadfeature.domain.model.Post
import com.dino.message.threadfeature.domain.model.PostMedia
import com.dino.message.threadfeature.domain.model.QuotedPostPreview
import com.dino.message.threadfeature.domain.usecase.CreatePostUseCase
import com.dino.message.threadfeature.domain.usecase.EditPostUseCase
import com.dino.message.threadfeature.domain.usecase.GetPostUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SelectedPostMedia(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val mimeType: String
)

class CreatePostViewModel constructor(
    app: Context,
    private val sPrefManager: SPrefManager,
    private val createPostUseCase: CreatePostUseCase,
    private val editPostUseCase: EditPostUseCase,
    private val getPostUseCase: GetPostUseCase,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {
    companion object {
        const val MAX_MEDIA_FILES_PER_POST = 8
        private const val MAX_VIDEO_DURATION_MS = 30_000L
    }

    private val quotedPostIdArg: Long = savedStateHandle.get<Long>("quotedPostId") ?: 0L
    private val editPostIdArg: Long = savedStateHandle.get<Long>("editPostId") ?: 0L
    val quotedPostId: Long? = quotedPostIdArg.takeIf { it > 0L }
    val editPostId: Long? = editPostIdArg.takeIf { it > 0L }
    val isEditMode: Boolean = editPostId != null

    val postDraft = mutableStateOf("")
    val followersOnly = mutableStateOf(false)
    private val _selectedMedia = mutableStateListOf<SelectedPostMedia>()
    val selectedMedia: List<SelectedPostMedia> = _selectedMedia
    private val _existingMedia = mutableStateListOf<PostMedia>()
    val existingMedia: List<PostMedia> = _existingMedia
    private var mediaIdSequence = 0L

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

    fun onMediaPicked(uris: List<Uri>) {
        if (uris.isEmpty()) return

        val resolver = getApplication<DinoOrderApplication>().contentResolver
        var skippedUnsupported = 0
        var skippedBlockedForSecurity = 0
        var skippedMaxLimit = 0
        var skippedVideoTooLong = 0
        var skippedUnknownVideoDuration = 0

        uris.forEach { uri ->
            if (_selectedMedia.size + _existingMedia.size >= MAX_MEDIA_FILES_PER_POST) {
                skippedMaxLimit++
                return@forEach
            }

            if (_selectedMedia.any { it.uri == uri }) {
                return@forEach
            }

            val mimeType = MediaFileResolver.resolveMimeType(
                contentResolver = resolver,
                uri = uri,
            )
            if (MediaMimePolicy.isBlockedForSecurity(mimeType)) {
                skippedBlockedForSecurity++
                return@forEach
            }
            if (!MediaMimePolicy.isSupportedUploadMimeType(mimeType)) {
                skippedUnsupported++
                return@forEach
            }
            if (mimeType.startsWith("video/")) {
                val durationMs = getVideoDurationMs(uri)
                if (durationMs == null) {
                    skippedUnknownVideoDuration++
                    return@forEach
                }
                if (durationMs > MAX_VIDEO_DURATION_MS) {
                    skippedVideoTooLong++
                    return@forEach
                }
            }

            mediaIdSequence += 1
            _selectedMedia.add(
                SelectedPostMedia(
                    id = mediaIdSequence,
                    uri = uri,
                    displayName = queryDisplayName(uri) ?: "media_$mediaIdSequence",
                    mimeType = mimeType
                )
            )
        }

        _error.value = when {
            skippedMaxLimit > 0 -> "Maximum 8 media files allowed"
            skippedVideoTooLong > 0 -> "Video must be 30 seconds or shorter"
            skippedUnknownVideoDuration > 0 -> "Could not determine video duration"
            skippedBlockedForSecurity > 0 -> "Some files were skipped for security reasons"
            skippedUnsupported > 0 -> "Some files were skipped: unsupported media type"
            else -> null
        }
    }

    fun removeSelectedMedia(id: Long) {
        _selectedMedia.removeAll { it.id == id }
    }

    fun removeExistingMedia(id: Long) {
        _existingMedia.removeAll { it.id == id }
    }

    fun submitPost() {
        if (_isSubmitting.value) return

        val text = postDraft.value.trim()
        val totalMediaCount = _existingMedia.size + _selectedMedia.size
        if (!isEditMode && text.isEmpty()) {
            _error.value = "Post text is required"
            return
        }
        if (isEditMode && text.isEmpty() && totalMediaCount == 0) {
            _error.value = "Text is required"
            return
        }

        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val mediaFiles = withContext(Dispatchers.IO) {
                    buildCreatePostMedia()
                }
                if (mediaFiles == null) {
                    _error.value = "Failed to read selected media files"
                    return@launch
                }

                val submitFlow = if (isEditMode) {
                    editPostUseCase.execute(
                        postId = editPostId ?: return@launch,
                        text = text,
                        mediaFiles = mediaFiles,
                        keepMediaIds = _existingMedia.map { it.id },
                        followersOnly = null
                    )
                } else {
                    createPostUseCase.execute(
                        text = text,
                        mediaFiles = mediaFiles,
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
                            _existingMedia.clear()
                            _selectedMedia.clear()
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
                        followersOnly.value = false
                        _existingMedia.clear()
                        _existingMedia.addAll(
                            res.data.media.filterNot { it.mime_type.startsWith("audio/", ignoreCase = true) }
                        )
                    }

                    is Resource.Error -> {
                        _isInitialPostLoading.value = false
                        _error.value = res.message.asString(getApplication())
                    }
                }
            }
        }
    }

    private fun buildCreatePostMedia(): List<CreatePostMedia>? {
        val resolver = getApplication<DinoOrderApplication>().contentResolver
        val result = mutableListOf<CreatePostMedia>()

        selectedMedia.forEach { media ->
            val canRead = try {
                resolver.openInputStream(media.uri)?.use { true } ?: false
            } catch (_: Exception) {
                false
            }
            if (!canRead) return null

            result.add(
                CreatePostMedia(
                    uri = media.uri,
                    fileName = media.displayName,
                    mimeType = media.mimeType,
                    sizeBytes = queryFileSize(media.uri),
                )
            )
        }

        return result
    }

    private fun queryDisplayName(uri: Uri): String? {
        val resolver = getApplication<DinoOrderApplication>().contentResolver
        return MediaFileResolver.queryDisplayName(resolver, uri)
    }

    private fun queryFileSize(uri: Uri): Long? {
        val resolver = getApplication<DinoOrderApplication>().contentResolver
        return MediaFileResolver.resolveFileSize(resolver, uri)
    }

    private fun getVideoDurationMs(uri: Uri): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(getApplication<DinoOrderApplication>(), uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
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
