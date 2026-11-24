package com.dino.message.corefeature.domain.upload

import io.ktor.utils.io.core.Input

fun interface UploadInputProvider {
    fun open(): Input
}

data class MediaFileHandle(
    val id: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long? = null,
)

data class ReadableMediaFile(
    val handle: MediaFileHandle,
    val openInput: UploadInputProvider,
    val metadata: PlatformMediaMetadata? = null,
    /** Release owned resources after uploads and retries have finished. */
    val release: () -> Unit = {},
)
