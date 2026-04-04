package com.dino.message.groupfeature.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddGroupMembersRequest(
    @SerialName("usernames") val usernames: List<String>,
)
