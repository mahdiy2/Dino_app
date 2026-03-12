package com.dino.message.threadfeature.domain.model

data class CreatePostMedia(
    val bytes: ByteArray,
    val fileName: String,
    val mimeType: String
)
