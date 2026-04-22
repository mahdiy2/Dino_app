package com.dino.message.threadfeature.presentation.viewmodel

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.threadfeature.domain.model.CreatePostMedia
import com.dino.message.threadfeature.domain.model.Post
import com.dino.message.threadfeature.domain.model.QuotedPostPreview
import com.dino.message.threadfeature.domain.usecase.CreatePostUseCase
import com.dino.message.threadfeature.domain.usecase.GetPostUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SelectedPostMedia(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val mimeType: String
)

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    @ApplicationContext app: Context,
    private val createPostUseCase: CreatePostUseCase,
    private val getPostUseCase: GetPostUseCase,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {
    companion object {
        const val MAX_MEDIA_FILES_PER_POST = 8
        private const val MAX_VIDEO_DURATION_MS = 30_000L
        private val SUPPORTED_MIME_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "image/webp",
            "video/mp4",
            "video/quicktime"
        )
    }

    private val quotedPostIdArg: Long = savedStateHandle.get<Long>("quotedPostId") ?: 0L
    val quotedPostId: Long? = quotedPostIdArg.takeIf { it > 0L }

    val postDraft = mutableStateOf("")
    val followersOnly = mutableStateOf(false)
    private val _selectedMedia = mutableStateListOf<SelectedPostMedia>()
    val selectedMedia: List<SelectedPostMedia> = _selectedMedia
    private var mediaIdSequence = 0L

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _quotedPost = MutableStateFlow<QuotedPostPreview?>(null)
    val quotedPost: StateFlow<QuotedPostPreview?> = _quotedPost.asStateFlow()
    private val _isQuotedPostLoading = MutableStateFlow(false)
    val isQuotedPostLoading: StateFlow<Boolean> = _isQuotedPostLoading.asStateFlow()
    private val _quotedPostError = MutableStateFlow<String?>(null)
    val quotedPostError: StateFlow<String?> = _quotedPostError.asStateFlow()

    init {
        loadQuotedPostPreviewIfNeeded()
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
        var skippedMaxLimit = 0
        var skippedVideoTooLong = 0
        var skippedUnknownVideoDuration = 0

        uris.forEach { uri ->
            if (_selectedMedia.size >= MAX_MEDIA_FILES_PER_POST) {
                skippedMaxLimit++
                return@forEach
            }

            if (_selectedMedia.any { it.uri == uri }) {
                return@forEach
            }

            val mimeType = resolver.getType(uri)?.lowercase().orEmpty()
            if (mimeType !in SUPPORTED_MIME_TYPES) {
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
            skippedUnsupported > 0 -> "Some files were skipped: unsupported media type"
            else -> null
        }
    }

    fun removeSelectedMedia(id: Long) {
        _selectedMedia.removeAll { it.id == id }
    }

    fun submitPost() {
        if (_isSubmitting.value) return

        val text = postDraft.value.trim()
        if (text.isEmpty()) {
            _error.value = "Post text is required"
            return
        }

        viewModelScope.launch {
            val mediaFiles = withContext(Dispatchers.IO) {
                buildCreatePostMedia()
            }
            if (mediaFiles == null) {
                _error.value = "Failed to read selected media files"
                _isSubmitting.value = false
                return@launch
            }

            createPostUseCase.execute(
                text = text,
                mediaFiles = mediaFiles,
                followersOnly = followersOnly.value,
                quotedPostId = quotedPostId
            ).collect { res ->
                when (res) {
                    is Resource.Success -> {
                        _isSubmitting.value = false
                        _error.value = null
                        postDraft.value = ""
                        followersOnly.value = false
                        _selectedMedia.clear()
                        navigateUp(viewModelScope)
                    }

                    is Resource.Loading -> {
                        _isSubmitting.value = true
                    }

                    is Resource.Error -> {
                        _isSubmitting.value = false
                        _error.value = res.message.asString(getApplication())
                    }
                }
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
        val cursor = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        return cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex == -1 || !it.moveToFirst()) return@use null
            it.getString(nameIndex)
        }
    }

    private fun queryFileSize(uri: Uri): Long? {
        val resolver = getApplication<DinoOrderApplication>().contentResolver
        val cursor = resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
        return cursor?.use {
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex == -1 || !it.moveToFirst()) return@use null
            if (it.isNull(sizeIndex)) return@use null
            it.getLong(sizeIndex)
        }
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
        media = media
    )
}
