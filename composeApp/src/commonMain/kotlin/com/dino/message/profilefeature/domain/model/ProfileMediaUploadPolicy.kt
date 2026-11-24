package com.dino.message.profilefeature.domain.model

import com.dino.message.corefeature.domain.model.StringResourceContent
import com.dino.message.corefeature.domain.model.asStringResourceContent
import com.dino.message.corefeature.domain.upload.MediaMimePolicy
import com.dino.message.corefeature.domain.upload.UploadMediaCategory

object ProfileMediaUploadPolicy {
    private const val MAX_VIDEO_SIZE_BYTES = 15L * 1024L * 1024L
    private const val MAX_VIDEO_DURATION_MS = 5_000L

    fun validateImage(mimeType: String): StringResourceContent? =
        MediaMimePolicy.validateUploadMimeType(mimeType, setOf(UploadMediaCategory.Image))
            ?.let { "Unsupported profile image type".asStringResourceContent() }

    fun validateVideo(
        mimeType: String,
        sizeBytes: Long?,
        durationMs: Long?,
        requireDuration: Boolean = false,
    ): StringResourceContent? {
        val mime = MediaMimePolicy.normalizeMimeType(mimeType)
        if (MediaMimePolicy.validateUploadMimeType(mime, setOf(UploadMediaCategory.Video)) != null) {
            return "Unsupported profile video type".asStringResourceContent()
        }
        if (mime == "video/mp4" || mime == "video/quicktime") {
            if (durationMs == null && requireDuration) {
                return "Could not determine profile video duration".asStringResourceContent()
            }
            if (durationMs != null && durationMs > MAX_VIDEO_DURATION_MS) {
                return "Profile video must be 5 seconds or shorter".asStringResourceContent()
            }
        }
        if (sizeBytes != null && sizeBytes > MAX_VIDEO_SIZE_BYTES) {
            return "Profile video is too large. Maximum allowed size is 15.0MB.".asStringResourceContent()
        }
        return null
    }
}
