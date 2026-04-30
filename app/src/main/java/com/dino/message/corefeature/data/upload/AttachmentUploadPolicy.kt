package com.dino.message.corefeature.data.upload

object AttachmentUploadPolicy {
    const val MAX_IMAGE_SIZE_BYTES: Long = 10L * 1024L * 1024L
    const val MAX_VIDEO_SIZE_BYTES: Long = 50L * 1024L * 1024L
    const val MAX_AUDIO_SIZE_BYTES: Long = 16L * 1024L * 1024L

    fun normalizeMimeType(rawMimeType: String?): String {
        return MediaMimePolicy.normalizeMimeType(rawMimeType)
    }

    fun validate(mimeType: String, fileSizeBytes: Long?): String? {
        val normalizedMimeType = normalizeMimeType(mimeType)
        if (normalizedMimeType.isBlank()) {
            return "Attachment mime type is required"
        }
        if (MediaMimePolicy.isBlockedForSecurity(normalizedMimeType)) {
            return "Unsupported attachment type for security reasons: $normalizedMimeType"
        }
        if (!MediaMimePolicy.isSupportedUploadMimeType(normalizedMimeType)) {
            return "Unsupported attachment type: $normalizedMimeType"
        }

        val maxSize = maxSizeForMimeType(normalizedMimeType) ?: return null
        if (fileSizeBytes != null && fileSizeBytes > maxSize) {
            return "${attachmentLabel(normalizedMimeType)} too large. Maximum allowed is ${formatMb(maxSize)}."
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

    private fun attachmentLabel(mimeType: String): String {
        return when (MediaMimePolicy.mediaCategory(mimeType)) {
            "image" -> "Image"
            "video" -> "Video"
            "audio" -> "Voice"
            else -> "File"
        }
    }

    private fun formatMb(sizeBytes: Long): String {
        return "${sizeBytes / (1024L * 1024L)}MB"
    }
}
