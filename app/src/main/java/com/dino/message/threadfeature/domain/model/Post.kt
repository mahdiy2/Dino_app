package com.dino.message.threadfeature.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: Long,
    val text: String,
    val author: Author,
    val created_at: String,
    val media: List<PostMedia>
)
