package com.dino.message.corefeature.domain.upload

import com.dino.message.corefeature.domain.model.AppStringKey
import com.dino.message.corefeature.domain.model.StringResourceContent
import com.dino.message.corefeature.domain.model.asStringResourceContent

object AttachmentUploadPolicy {
    const val MAX_IMAGE_SIZE_BYTES: Long = 10L * 1024L * 1024L
    const val MAX_VIDEO_SIZE_BYTES: Long = 50L * 1024L * 1024L
    const val MAX_AUDIO_SIZE_BYTES: Long = 16L * 1024L * 1024L

    fun normalizeMimeType(rawMimeType: String?): String {
        return MediaMimePolicy.normalizeMimeType(rawMimeType)
    }

    fun validate(mimeType: String, fileSizeBytes: Long?): StringResourceContent? {
        val normalizedMimeType = normalizeMimeType(mimeType)
        if (normalizedMimeType.isBlank()) {
            return AppStringKey.AttachmentMimeTypeRequired.asStringResourceContent()
        }
        if (MediaMimePolicy.isBlockedForSecurity(normalizedMimeType)) {
            return AppStringKey.UnsupportedAttachmentTypeSecurity.asStringResourceContent(normalizedMimeType)
        }
        if (!MediaMimePolicy.isSupportedUploadMimeType(normalizedMimeType)) {
            return AppStringKey.UnsupportedAttachmentType.asStringResourceContent(normalizedMimeType)
        }

        val maxSize = maxSizeForMimeType(normalizedMimeType) ?: return null
        if (fileSizeBytes != null && fileSizeBytes > maxSize) {
            return attachmentTooLargeKey(normalizedMimeType).asStringResourceContent(
                formatMb(maxSize),
            )
        }
        return null
    }

    fun maxSizeForMimeType(mimeType: String): Long? {
        return when (MediaMimePolicy.mediaCategory(mimeType)) {
            "image" -> MAX_IMAGE_SIZE_BYTES
            "video" -> MAX_VIDEO_SIZE_BYTES
            "audio" -> MAX_AUDIO_SIZE_BYTES
            else -> null
        }
    }

    private fun attachmentTooLargeKey(mimeType: String): AppStringKey {
        return when (MediaMimePolicy.mediaCategory(mimeType)) {
            "image" -> AppStringKey.ImageAttachmentTooLarge
            "video" -> AppStringKey.VideoAttachmentTooLarge
            "audio" -> AppStringKey.VoiceAttachmentTooLarge
            else -> AppStringKey.FileAttachmentTooLarge
        }
    }

    private fun formatMb(sizeBytes: Long): String {
        return "${sizeBytes / (1024L * 1024L)}MB"
    }
}
