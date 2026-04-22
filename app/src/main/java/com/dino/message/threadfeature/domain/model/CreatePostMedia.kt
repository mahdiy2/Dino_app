package com.dino.message.threadfeature.domain.model

import android.net.Uri

data class CreatePostMedia(
    val uri: Uri,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long? = null,
    val trackTitle: String? = null,
    val trackArtist: String? = null,
)
