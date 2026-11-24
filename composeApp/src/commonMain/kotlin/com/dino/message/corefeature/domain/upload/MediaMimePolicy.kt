package com.dino.message.corefeature.domain.upload

import com.dino.message.corefeature.domain.model.AppStringKey
import com.dino.message.corefeature.domain.model.StringResourceContent
import com.dino.message.corefeature.domain.model.asStringResourceContent

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

    fun validateUploadMimeType(
        mimeType: String,
        allowedCategories: Set<UploadMediaCategory> = UploadMediaCategory.entries.toSet(),
    ): StringResourceContent? {
        val normalized = normalizeMimeType(mimeType)
        return when {
            normalized.isBlank() -> AppStringKey.AttachmentMimeTypeRequired.asStringResourceContent()
            isBlockedForSecurity(normalized) ->
                AppStringKey.UnsupportedAttachmentTypeSecurity.asStringResourceContent(normalized)
            allowedCategories.none { mediaCategory(normalized) == it.mimePrefix } ->
                AppStringKey.UnsupportedAttachmentType.asStringResourceContent(normalized)
            else -> null
        }
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
