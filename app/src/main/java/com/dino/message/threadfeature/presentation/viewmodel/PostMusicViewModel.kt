package com.dino.message.threadfeature.presentation.viewmodel

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.data.upload.MediaMimePolicy
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.threadfeature.data.upload.PostUploadManager
import com.dino.message.threadfeature.domain.model.CreatePostMedia
import com.dino.message.threadfeature.domain.model.Post
import com.dino.message.threadfeature.domain.model.QuotedPostPreview
import com.dino.message.threadfeature.domain.usecase.GetPostUseCase
import com.dino.message.threadfeature.presentation.util.resolveMusicMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SelectedMusicTrack(
    val uri: Uri,
    val displayName: String,
    val title: String,
    val artist: String?,
    val mimeType: String
)

@HiltViewModel
class PostMusicViewModel @Inject constructor(
    @ApplicationContext app: Context,
    private val sPrefManager: SPrefManager,
    private val postUploadManager: PostUploadManager,
    private val getPostUseCase: GetPostUseCase,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    private val quotedPostIdArg: Long = savedStateHandle.get<Long>("quotedPostId") ?: 0L
    val quotedPostId: Long? = quotedPostIdArg.takeIf { it > 0L }

    val postDraft = mutableStateOf("")
    val followersOnly = mutableStateOf(false)
    val selectedTrack = mutableStateOf<SelectedMusicTrack?>(null)

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

    fun onMusicPicked(uri: Uri) {
        val resolver = getApplication<DinoOrderApplication>().contentResolver
        val displayName = queryDisplayName(uri) ?: "music_track"
        val mimeType = resolveMusicMimeType(uri, displayName)
        if (mimeType == null) {
            _error.value = "Selected file is not a supported music file"
            return
        }

        val (rawTitle, rawArtist) = extractMetadata(uri)
        val resolved = resolveMusicMetadata(
            displayName = displayName,
            title = rawTitle,
            artist = rawArtist,
            titleFallback = "Unknown track"
        )
        selectedTrack.value = SelectedMusicTrack(
            uri = uri,
            displayName = displayName,
            title = resolved.title,
            artist = resolved.artist,
            mimeType = mimeType
        )
        _error.value = null
    }

    fun clearSelectedTrack() {
        selectedTrack.value = null
    }

    fun submitPost() {
        if (_isSubmitting.value) return

        val track = selectedTrack.value
        if (track == null) {
            _error.value = "Please select one music track"
            return
        }

        val text = postDraft.value.trim()

        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val media = withContext(Dispatchers.IO) {
                    buildMedia(track)
                }

                if (media == null) {
                    _error.value = "Failed to read selected music file"
                    return@launch
                }

                val username = sPrefManager.getUsername().orEmpty().trim()
                if (username.isBlank()) {
                    _error.value = "Session expired. Please log in again."
                    return@launch
                }

                val hasInFlightUpload = withContext(Dispatchers.IO) {
                    postUploadManager.hasInFlightUpload(username)
                }
                if (hasInFlightUpload) {
                    _error.value = "Another post upload is already in progress. Please wait."
                    return@launch
                }

                val workId = postUploadManager.enqueueUpload(
                    username = username,
                    text = text,
                    followersOnly = followersOnly.value,
                    quotedPostId = quotedPostId,
                    mediaUris = listOf(media.uri.toString()),
                    mediaNames = listOf(media.fileName),
                    mediaMimeTypes = listOf(media.mimeType),
                    trackTitle = media.trackTitle,
                    trackArtist = media.trackArtist,
                )
                if (workId == null) {
                    _error.value = "Another post upload is already in progress. Please wait."
                    return@launch
                }

                _error.value = null
                postDraft.value = ""
                followersOnly.value = false
                selectedTrack.value = null
                navigateUp(viewModelScope)
            } catch (_: Exception) {
                _error.value = "Failed to queue post upload. Please try again."
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

    private fun buildMedia(track: SelectedMusicTrack): CreatePostMedia? {
        val resolver = getApplication<DinoOrderApplication>().contentResolver
        val canRead = try {
            resolver.openInputStream(track.uri)?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
        if (!canRead) return null

        return CreatePostMedia(
            uri = track.uri,
            fileName = track.displayName,
            mimeType = track.mimeType,
            sizeBytes = queryFileSize(track.uri),
            trackTitle = track.title,
            trackArtist = track.artist
        )
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

    private fun extractMetadata(uri: Uri): Pair<String?, String?> {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(getApplication<DinoOrderApplication>(), uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            title to artist
        } catch (_: Exception) {
            null to null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun resolveMusicMimeType(uri: Uri, displayName: String): String? {
        val resolver = getApplication<DinoOrderApplication>().contentResolver
        val rawResolverMime = MediaMimePolicy.normalizeMimeType(resolver.getType(uri))
        if (MediaMimePolicy.isBlockedForSecurity(rawResolverMime)) {
            return null
        }
        if (rawResolverMime.startsWith("audio/")) {
            return rawResolverMime
        }

        val retrieverMime = extractRetrieverMimeType(uri)
        if (retrieverMime.startsWith("audio/")) {
            return retrieverMime
        }

        val extension = displayName
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

    private fun extractRetrieverMimeType(uri: Uri): String {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(getApplication<DinoOrderApplication>(), uri)
            MediaMetadataRetriever.METADATA_KEY_MIMETYPE
                .let(retriever::extractMetadata)
                .orEmpty()
                .lowercase()
        } catch (_: Exception) {
            ""
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
