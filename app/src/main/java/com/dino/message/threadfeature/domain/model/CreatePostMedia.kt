package com.dino.message.threadfeature.domain.model

data class CreatePostMedia(
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String,
    val trackTitle: String? = null,
    val trackArtist: String? = null,
)
