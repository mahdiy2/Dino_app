package com.dino.message.profilefeature.presentation.viewmodel

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.profilefeature.domain.model.ProfileUpdateImage
import com.dino.message.profilefeature.domain.usecase.GetMyProfileUseCase
import com.dino.message.profilefeature.domain.usecase.UpdateMyProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SelectedProfileMedia(
    val uri: Uri,
    val displayName: String,
    val mimeType: String
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    @ApplicationContext app: Context,
    private val getMyProfileUseCase: GetMyProfileUseCase,
    private val updateMyProfileUseCase: UpdateMyProfileUseCase
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {
    companion object {
        private const val MAX_PROFILE_VIDEO_DURATION_MS = 5_000L
        private const val MAX_PROFILE_VIDEO_SIZE_BYTES = 15L * 1024L * 1024L
        private val SUPPORTED_IMAGE_MIME_TYPES = setOf(
            "image/jpeg",
            "image/png",
            "image/webp"
        )
        private val SUPPORTED_VIDEO_MIME_TYPES = setOf(
            "video/mp4",
            "video/quicktime"
        )
    }

    val nameDraft = mutableStateOf("")
    val bioDraft = mutableStateOf("")
    val selectedImage = mutableStateOf<SelectedProfileMedia?>(null)
    val selectedVideo = mutableStateOf<SelectedProfileMedia?>(null)
    val currentProfileImageUrl = mutableStateOf<String?>(null)
    val currentProfileVideoUrl = mutableStateOf<String?>(null)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadProfile()
    }

    fun onNameChanged(value: String) {
        nameDraft.value = value
        if (_error.value != null) _error.value = null
    }

    fun onBioChanged(value: String) {
        bioDraft.value = value
        if (_error.value != null) _error.value = null
    }

    fun onImagePicked(uri: Uri?) {
        if (uri == null) return
        val resolver = getApplication<DinoOrderApplication>().contentResolver
        val mimeType = resolver.getType(uri)?.lowercase().orEmpty()
        if (mimeType !in SUPPORTED_IMAGE_MIME_TYPES) {
            _error.value = "Unsupported profile image type"
            return
        }

        selectedImage.value = SelectedProfileMedia(
            uri = uri,
            displayName = queryDisplayName(uri) ?: "profile_image",
            mimeType = mimeType
        )
        _error.value = null
    }

    fun onVideoPicked(uri: Uri?) {
        if (uri == null) return
        val resolver = getApplication<DinoOrderApplication>().contentResolver
        val mimeType = resolver.getType(uri)?.lowercase().orEmpty()
        if (mimeType !in SUPPORTED_VIDEO_MIME_TYPES) {
            _error.value = "Unsupported profile video type"
            return
        }

        val durationMs = getVideoDurationMs(uri)
        if (durationMs == null) {
            _error.value = "Could not determine profile video duration"
            return
        }
        if (durationMs > MAX_PROFILE_VIDEO_DURATION_MS) {
            _error.value = "Profile video must be 5 seconds or shorter"
            return
        }

        val size = queryFileSize(uri)
        if (size != null && size > MAX_PROFILE_VIDEO_SIZE_BYTES) {
            _error.value = "Profile video is too large. Maximum allowed size is 15.0MB."
            return
        }

        selectedVideo.value = SelectedProfileMedia(
            uri = uri,
            displayName = queryDisplayName(uri) ?: "profile_video",
            mimeType = mimeType
        )
        _error.value = null
    }

    fun clearSelectedImage() {
        selectedImage.value = null
    }

    fun clearSelectedVideo() {
        selectedVideo.value = null
    }

    fun closeScreen() {
        if (_isSaving.value) return
        navigateUp(viewModelScope)
    }

    fun saveProfile() {
        if (_isSaving.value) return

        viewModelScope.launch {
            val imagePayload = withContext(Dispatchers.IO) {
                selectedImage.value?.let { buildProfileUpdateMedia(it) }
            }
            if (selectedImage.value != null && imagePayload == null) {
                _error.value = "Failed to read selected profile image"
                _isSaving.value = false
                return@launch
            }

            val videoPayload = withContext(Dispatchers.IO) {
                selectedVideo.value?.let { buildProfileUpdateMedia(it) }
            }
            if (selectedVideo.value != null && videoPayload == null) {
                _error.value = "Failed to read selected profile video"
                _isSaving.value = false
                return@launch
            }
            if (videoPayload != null && videoPayload.bytes.size > MAX_PROFILE_VIDEO_SIZE_BYTES) {
                _error.value = "Profile video is too large. Maximum allowed size is 15.0MB."
                _isSaving.value = false
                return@launch
            }

            updateMyProfileUseCase.execute(
                name = nameDraft.value.trim().ifEmpty { null },
                bio = bioDraft.value,
                profileImage = imagePayload,
                profileVideo = videoPayload
            ).collect { res ->
                when (res) {
                    is Resource.Success -> {
                        _isSaving.value = false
                        _error.value = null
                        navigateUp(viewModelScope)
                    }

                    is Resource.Loading -> {
                        _isSaving.value = true
                    }

                    is Resource.Error -> {
                        _isSaving.value = false
                        _error.value = res.message.asString(getApplication())
                    }
                }
            }
        }
    }

    private fun loadProfile() = viewModelScope.launch {
        getMyProfileUseCase.execute().collect { res ->
            when (res) {
                is Resource.Success -> {
                    _isLoading.value = false
                    nameDraft.value = res.data.name.orEmpty()
                    bioDraft.value = res.data.bio.orEmpty()
                    currentProfileImageUrl.value = res.data.profile_image_url
                    currentProfileVideoUrl.value = res.data.profile_video_url
                    _error.value = null
                }

                is Resource.Loading -> {
                    _isLoading.value = true
                }

                is Resource.Error -> {
                    _isLoading.value = false
                    _error.value = res.message.asString(getApplication())
                }
            }
        }
    }

    private fun buildProfileUpdateMedia(media: SelectedProfileMedia): ProfileUpdateImage? {
        val resolver = getApplication<DinoOrderApplication>().contentResolver
        val bytes = try {
            resolver.openInputStream(media.uri)?.use { it.readBytes() }
        } catch (_: Exception) {
            null
        } ?: return null

        return ProfileUpdateImage(
            bytes = bytes,
            fileName = media.displayName,
            mimeType = media.mimeType
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
}
