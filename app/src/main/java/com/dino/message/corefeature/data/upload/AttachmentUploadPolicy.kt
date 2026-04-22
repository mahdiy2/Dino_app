package com.dino.message.corefeature.data.upload

object AttachmentUploadPolicy {
    const val MAX_IMAGE_SIZE_BYTES: Long = 10L * 1024L * 1024L
    const val MAX_VIDEO_SIZE_BYTES: Long = 50L * 1024L * 1024L
    const val MAX_AUDIO_SIZE_BYTES: Long = 16L * 1024L * 1024L

    private val imageMimeTypes = setOf(
        "image/jpeg",
        "image/png",
        "image/webp",
    )
    private val videoMimeTypes = setOf(
        "video/mp4",
        "video/quicktime",
    )
    private val audioMimeTypes = setOf(
        "audio/mpeg",
        "audio/mp3",
        "audio/wav",
        "audio/webm",
        "audio/ogg",
        "audio/aac",
        "audio/mp4",
    )

    private val allMimeTypes = imageMimeTypes + videoMimeTypes + audioMimeTypes

    fun normalizeMimeType(rawMimeType: String?): String {
        return rawMimeType
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase()
            .orEmpty()
    }

    fun validate(mimeType: String, fileSizeBytes: Long?): String? {
        val normalizedMimeType = normalizeMimeType(mimeType)
        if (normalizedMimeType.isBlank()) {
            return "Attachment mime type is required"
        }
        if (normalizedMimeType !in allMimeTypes) {
            return "Unsupported attachment type: $normalizedMimeType"
        }

        val maxSize = maxSizeForMimeType(normalizedMimeType) ?: return null
        if (fileSizeBytes != null && fileSizeBytes > maxSize) {
            return "${attachmentLabel(normalizedMimeType)} too large. Maximum allowed is ${formatMb(maxSize)}."
        }
        return null
    }

    fun maxSizeForMimeType(mimeType: String): Long? {
        val normalizedMimeType = normalizeMimeType(mimeType)
        return when {
            normalizedMimeType in imageMimeTypes -> MAX_IMAGE_SIZE_BYTES
            normalizedMimeType in videoMimeTypes -> MAX_VIDEO_SIZE_BYTES
            normalizedMimeType in audioMimeTypes -> MAX_AUDIO_SIZE_BYTES
            else -> null
        }
    }

    private fun attachmentLabel(mimeType: String): String {
        val normalizedMimeType = normalizeMimeType(mimeType)
        return when {
            normalizedMimeType in imageMimeTypes -> "Image"
            normalizedMimeType in videoMimeTypes -> "Video"
            normalizedMimeType in audioMimeTypes -> "Voice"
            else -> "File"
        }
    }

    private fun formatMb(sizeBytes: Long): String {
        return "${sizeBytes / (1024L * 1024L)}MB"
    }
}
