package com.dino.message.threadfeature.domain.model

import com.dino.message.corefeature.domain.upload.ReadableMediaFile

const val MAX_POST_VIDEO_DURATION_MS = 30_000L

data class CreatePostMedia(
    val file: ReadableMediaFile,
    val trackTitle: String? = file.metadata?.title,
    val trackArtist: String? = file.metadata?.artist,
) {
    val fileName: String get() = file.handle.displayName
    val mimeType: String get() = file.handle.mimeType
    val sizeBytes: Long? get() = file.handle.sizeBytes
}
