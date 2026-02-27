package com.dino.message.threadfeature.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class VoteRequest(
    val target_type: String,
    val target_id: Long,
    val value: Long
)
