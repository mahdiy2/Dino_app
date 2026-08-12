package com.dino.message.threadfeature.domain.model

import com.dino.message.corefeature.data.upload.ReadableMediaFile

data class CreatePostMedia(
    val file: ReadableMediaFile,
    val trackTitle: String? = null,
    val trackArtist: String? = null,
) {
    val fileName: String get() = file.handle.displayName
    val mimeType: String get() = file.handle.mimeType
    val sizeBytes: Long? get() = file.handle.sizeBytes
}
