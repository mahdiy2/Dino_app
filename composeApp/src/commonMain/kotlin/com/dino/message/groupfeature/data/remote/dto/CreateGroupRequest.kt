package com.dino.message.groupfeature.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateGroupRequest(
    val name: String,
    val members: List<String>,
)
