package com.dino.message.profilefeature.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.data.spref.DataStoreUtil
import com.dino.message.corefeature.data.upload.AndroidUploadContentProvider
import com.dino.message.corefeature.data.upload.MediaFileResolver
import com.dino.message.corefeature.data.upload.MediaMimePolicy
import com.dino.message.corefeature.presentation.platform.AndroidMediaMetadataExtractor
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.util.PROFILE_IMAGE_SHAPE_CIRCLE
import com.dino.message.corefeature.presentation.util.hasBadgeAccess
import com.dino.message.corefeature.presentation.util.normalizeProfileImageShape
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.profilefeature.domain.model.ProfileUpdateImage
import com.dino.message.profilefeature.domain.usecase.GetMyProfileUseCase
import com.dino.message.profilefeature.domain.usecase.UpdateMyProfileUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SelectedProfileMedia(
    val uri: Uri,
    val displayName: String,
    val mimeType: String
)

class EditProfileViewModel constructor(
    app: Context,
    private val dataStoreUtil: DataStoreUtil,
    private val getMyProfileUseCase: GetMyProfileUseCase,
    private val updateMyProfileUseCase: UpdateMyProfileUseCase,
    private val uploadContentProvider: AndroidUploadContentProvider
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {
    private val mediaMetadataExtractor = AndroidMediaMetadataExtractor(app)

    companion object {
        private const val MAX_PROFILE_VIDEO_DURATION_MS = 5_000L
        private const val MAX_PROFILE_VIDEO_SIZE_BYTES = 15L * 1024L * 1024L
    }

    val nameDraft = mutableStateOf("")
    val bioDraft = mutableStateOf("")
    val selectedImage = mutableStateOf<SelectedProfileMedia?>(null)
    val selectedVideo = mutableStateOf<SelectedProfileMedia?>(null)
    val currentProfileImageUrl = mutableStateOf<String?>(null)
    val currentProfileVideoUrl = mutableStateOf<String?>(null)
    val removeProfileImageRequested = mutableStateOf(false)
    val removeProfileVideoRequested = mutableStateOf(false)
    val selectedProfileImageShape = mutableStateOf(PROFILE_IMAGE_SHAPE_CIRCLE)
    val currentProfileBadge = mutableStateOf<String?>(null)

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
        val mimeType = MediaFileResolver.resolveMimeType(
            contentResolver = resolver,
            uri = uri,
        )
        if (!mimeType.startsWith("image/") ||
            !MediaMimePolicy.isSupportedUploadMimeType(mimeType)
        ) {
            _error.value = "Unsupported profile image type"
            return
        }

        selectedImage.value = SelectedProfileMedia(
            uri = uri,
            displayName = queryDisplayName(uri) ?: "profile_image",
            mimeType = mimeType
        )
        removeProfileImageRequested.value = false
        _error.value = null
    }

    fun onVideoPicked(uri: Uri?) {
        if (uri == null) return
        val resolver = getApplication<DinoOrderApplication>().contentResolver
        val mimeType = MediaFileResolver.resolveMimeType(
            contentResolver = resolver,
            uri = uri,
        )
        if (!mimeType.startsWith("video/") ||
            !MediaMimePolicy.isSupportedUploadMimeType(mimeType)
        ) {
            _error.value = "Unsupported profile video type"
            return
        }

        val durationMs = getVideoDurationMs(uri)
        if (mimeType == "video/mp4" || mimeType == "video/quicktime") {
            if (durationMs == null) {
                _error.value = "Could not determine profile video duration"
                return
            }
            if (durationMs > MAX_PROFILE_VIDEO_DURATION_MS) {
                _error.value = "Profile video must be 5 seconds or shorter"
                return
            }
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
        removeProfileVideoRequested.value = false
        _error.value = null
    }

    fun clearSelectedImage() {
        selectedImage.value = null
        removeProfileImageRequested.value = false
    }

    fun clearSelectedVideo() {
        selectedVideo.value = null
        removeProfileVideoRequested.value = false
    }

    fun removeProfileImage() {
        selectedImage.value = null
        currentProfileImageUrl.value = null
        removeProfileImageRequested.value = true
        if (_error.value != null) _error.value = null
    }

    fun removeProfileVideo() {
        selectedVideo.value = null
        currentProfileVideoUrl.value = null
        removeProfileVideoRequested.value = true
        if (_error.value != null) _error.value = null
    }

    fun onProfileImageShapeSelected(shape: String) {
        selectedProfileImageShape.value = normalizeProfileImageShape(shape)
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
            val videoSizeBytes = videoPayload?.sizeBytes
            if (videoSizeBytes != null && videoSizeBytes > MAX_PROFILE_VIDEO_SIZE_BYTES) {
                _error.value = "Profile video is too large. Maximum allowed size is 15.0MB."
                _isSaving.value = false
                return@launch
            }

            updateMyProfileUseCase.execute(
                name = nameDraft.value.trim().ifEmpty { null },
                bio = bioDraft.value,
                profileImageShape = selectedProfileImageShape.value.takeIf {
                    hasBadgeAccess(currentProfileBadge.value)
                },
                profileImage = imagePayload,
                profileVideo = videoPayload,
                removeProfileImage = removeProfileImageRequested.value,
                removeProfileVideo = removeProfileVideoRequested.value,
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
                    selectedProfileImageShape.value = normalizeProfileImageShape(res.data.profile_image_shape)
                    currentProfileBadge.value = res.data.badge
                    currentProfileVideoUrl.value = res.data.profile_video_url
                    removeProfileImageRequested.value = false
                    removeProfileVideoRequested.value = false
                    dataStoreUtil.saveBadgeAccessCache(
                        hasBadge = hasBadgeAccess(res.data.badge)
                    )
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
        val canRead = try {
            resolver.openInputStream(media.uri)?.use { true } ?: false
        } catch (_: Exception) {
            false
        }
        if (!canRead) return null

        return ProfileUpdateImage(
            file = uploadContentProvider.readableMediaFile(
                uri = media.uri,
                displayName = media.displayName,
                mimeType = media.mimeType,
                sizeBytes = queryFileSize(media.uri),
            ),
        )
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
        return mediaMetadataExtractor.extract(uri).durationMs
    }
}
