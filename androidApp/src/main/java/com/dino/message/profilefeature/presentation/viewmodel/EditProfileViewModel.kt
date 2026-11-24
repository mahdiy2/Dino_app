package com.dino.message.profilefeature.presentation.viewmodel

import com.dino.message.threadfeature.domain.model.CreatePostMedia
import com.dino.message.profilefeature.domain.model.Profile
import com.dino.message.corefeature.data.upload.AndroidMediaOutbox
import com.dino.message.corefeature.data.upload.MediaSubmissionCommand
import com.dino.message.corefeature.presentation.util.asString
import androidx.compose.runtime.mutableStateOf
import com.dino.message.corefeature.domain.settings.AppSettingsStore
import com.dino.message.corefeature.domain.system.AppClock
import com.dino.message.corefeature.domain.upload.PlatformMediaMetadata
import com.dino.message.profilefeature.domain.model.ProfileMediaUploadPolicy
import com.dino.message.corefeature.domain.upload.PlatformMediaReference
import com.dino.message.corefeature.domain.upload.PlatformMediaResolver
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.corefeature.domain.resource.StringResolver
import com.dino.message.corefeature.presentation.util.PROFILE_IMAGE_SHAPE_CIRCLE
import com.dino.message.corefeature.presentation.util.hasBadgeAccess
import com.dino.message.corefeature.presentation.util.normalizeProfileImageShape
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.corefeature.presentation.util.UiEventBus
import com.dino.message.corefeature.presentation.viewmodel.ScopedStateHolder
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
    val reference: PlatformMediaReference,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long? = null,
    val durationMs: Long? = null,
)

class EditProfileViewModel constructor(
    private val stringResolver: StringResolver,
    uiEventBus: UiEventBus,
    private val dataStoreUtil: AppSettingsStore,
    private val getMyProfileUseCase: GetMyProfileUseCase,
    private val mediaOutbox: AndroidMediaOutbox,
    private val updateMyProfileUseCase: UpdateMyProfileUseCase,
    private val mediaResolver: PlatformMediaResolver,
    private val appClock: AppClock,
) : ScopedStateHolder(),
    HaveUIEvent by HaveUIEventImpl(uiEventBus) {

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

    private val _queuedForUpload = MutableStateFlow(false)
    val queuedForUpload = _queuedForUpload.asStateFlow()

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

    fun onImagePicked(reference: PlatformMediaReference?) {
        if (reference == null) return
        val pickedMedia = mediaResolver.resolvePickedMedia(reference, displayNameHint = "profile_image")
        val mimeType = pickedMedia.mimeType
        ProfileMediaUploadPolicy.validateImage(mimeType)?.let {
            _error.value = it.resolveWith(stringResolver)
            return
        }

        selectedImage.value = SelectedProfileMedia(
            reference = pickedMedia.reference,
            displayName = pickedMedia.displayName,
            mimeType = mimeType,
            sizeBytes = pickedMedia.sizeBytes,
            durationMs = pickedMedia.durationMs,
        )
        removeProfileImageRequested.value = false
        _error.value = null
    }

    fun onVideoPicked(reference: PlatformMediaReference?) {
        if (reference == null) return
        val pickedMedia = mediaResolver.resolvePickedMedia(reference, displayNameHint = "profile_video")
        val mimeType = pickedMedia.mimeType
        ProfileMediaUploadPolicy.validateVideo(
            mimeType = mimeType,
            sizeBytes = pickedMedia.sizeBytes,
            durationMs = pickedMedia.durationMs,
            requireDuration = true,
        )?.let {
            _error.value = it.resolveWith(stringResolver)
            return
        }

        selectedVideo.value = SelectedProfileMedia(
            reference = pickedMedia.reference,
            displayName = pickedMedia.displayName,
            mimeType = mimeType,
            sizeBytes = pickedMedia.sizeBytes,
            durationMs = pickedMedia.durationMs,
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
            val submission = MediaSubmissionCommand.Profile(
                name = nameDraft.value.trim().ifEmpty { null },
                bio = bioDraft.value,
                shape = selectedProfileImageShape.value.takeIf { hasBadgeAccess(currentProfileBadge.value) },
                hasImage = imagePayload != null,
                hasVideo = videoPayload != null,
                removeImage = removeProfileImageRequested.value,
                removeVideo = removeProfileVideoRequested.value,
            )
            val submitImmediately = {
                updateMyProfileUseCase.execute(
                    name = submission.name, bio = submission.bio, profileImageShape = submission.shape,
                    profileImage = imagePayload, profileVideo = videoPayload,
                    removeProfileImage = submission.removeImage, removeProfileVideo = submission.removeVideo,
                )
            }
            val media = listOfNotNull(imagePayload, videoPayload).map { CreatePostMedia(it.file) }
            val updates = if (media.isEmpty()) submitImmediately()
            else mediaOutbox.submit(
                submission, media, Profile.serializer(),
                onQueued = { _queuedForUpload.value = true },
                legacy = submitImmediately,
            )
            updates.collect { res ->
                when (res) {
                    is Resource.Success -> {
                        _isSaving.value = false
                        _queuedForUpload.value = false
                        _error.value = null
                        navigateUp(viewModelScope)
                    }

                    is Resource.Loading -> {
                        _isSaving.value = true
                    }

                    is Resource.Error -> {
                        _isSaving.value = false
                        _queuedForUpload.value = false
                        _error.value = res.message.resolveWith(stringResolver)
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
                        hasBadge = hasBadgeAccess(res.data.badge),
                        syncedAtEpochMillis = appClock.currentTimeMillis(),
                    )
                    _error.value = null
                }

                is Resource.Loading -> {
                    _isLoading.value = true
                }

                is Resource.Error -> {
                    _isLoading.value = false
                    _error.value = res.message.resolveWith(stringResolver)
                }
            }
        }
    }

    private fun buildProfileUpdateMedia(media: SelectedProfileMedia): ProfileUpdateImage? {
        if (!mediaResolver.canRead(media.reference)) return null

        return ProfileUpdateImage(
            file = mediaResolver.readableMediaFile(
                source = media.reference,
                displayName = media.displayName,
                mimeType = media.mimeType,
                sizeBytes = media.sizeBytes,
            ).copy(metadata = PlatformMediaMetadata(durationMs = media.durationMs)),
        )
    }
}
