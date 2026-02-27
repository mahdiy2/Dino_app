package com.dino.message.threadfeature.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val id: Long,
    val author: Author,
    val text: String,
    val score: Long,
    val created_at: String,
    val parent_id: Long? = null,
    val replies: List<Comment> = emptyList()
)
