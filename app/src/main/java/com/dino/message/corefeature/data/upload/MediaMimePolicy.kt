package com.dino.message.corefeature.data.upload

object MediaMimePolicy {
    private val blockedMimeTypes = setOf(
        "image/svg+xml",
    )

    fun normalizeMimeType(rawMimeType: String?): String {
        return rawMimeType
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase()
            .orEmpty()
    }

    fun isBlockedForSecurity(mimeType: String): Boolean {
        val normalizedMimeType = normalizeMimeType(mimeType)
        return normalizedMimeType in blockedMimeTypes ||
            normalizedMimeType.startsWith("image/svg")
    }

    fun isSupportedUploadMimeType(mimeType: String): Boolean {
        val normalizedMimeType = normalizeMimeType(mimeType)
        if (normalizedMimeType.isBlank()) return false
        if (isBlockedForSecurity(normalizedMimeType)) return false
        return normalizedMimeType.startsWith("image/") ||
            normalizedMimeType.startsWith("video/") ||
            normalizedMimeType.startsWith("audio/")
    }

    fun mediaCategory(mimeType: String): String? {
        val normalizedMimeType = normalizeMimeType(mimeType)
        return when {
            normalizedMimeType.startsWith("image/") -> "image"
            normalizedMimeType.startsWith("video/") -> "video"
            normalizedMimeType.startsWith("audio/") -> "audio"
            else -> null
        }
    }
}
