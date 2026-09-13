package com.dino.message.profilefeature.domain.model

import com.dino.message.corefeature.domain.upload.ReadableMediaFile

data class ProfileUpdateImage(
    val file: ReadableMediaFile,
) {
    val fileName: String get() = file.handle.displayName
    val mimeType: String get() = file.handle.mimeType
    val sizeBytes: Long? get() = file.handle.sizeBytes
}
