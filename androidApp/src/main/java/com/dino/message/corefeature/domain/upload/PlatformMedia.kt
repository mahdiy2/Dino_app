package com.dino.message.corefeature.domain.upload

data class PlatformMediaReference(
    val value: String,
)

data class PlatformMediaMetadata(
    val durationMs: Long? = null,
    val title: String? = null,
    val artist: String? = null,
    val mimeType: String? = null,
)

data class PlatformPickedMedia(
    val reference: PlatformMediaReference,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long? = null,
    val durationMs: Long? = null,
)

interface PlatformMediaMetadataExtractor {
    fun extract(source: PlatformMediaReference): PlatformMediaMetadata
}

interface PlatformMediaResolver {
    fun canRead(source: PlatformMediaReference): Boolean
    fun resolvePickedMedia(
        source: PlatformMediaReference,
        displayNameHint: String? = null,
    ): PlatformPickedMedia
    fun metadata(source: PlatformMediaReference): PlatformMediaMetadata
    fun mimeTypeFromDisplayName(displayName: String): String?
    fun readableMediaFile(
        source: PlatformMediaReference,
        displayName: String? = null,
        mimeType: String? = null,
        sizeBytes: Long? = null,
    ): ReadableMediaFile
}
