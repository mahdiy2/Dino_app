package com.dino.message.threadfeature.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CreatedPost(
    val post_id: Long,
    val message: String
)
