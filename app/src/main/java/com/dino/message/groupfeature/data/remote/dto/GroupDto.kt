package com.dino.message.groupfeature.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupCreatorDto(
    val id: Int,
    val username: String,
    val name: String? = null,
    @SerialName("profile_image_url") val profileImageUrl: String? = null,
)

@Serializable
data class GroupMemberDto(
    val id: Int,
    val username: String,
    val name: String? = null,
    @SerialName("profile_image_url") val profileImageUrl: String? = null,
    @SerialName("public_key") val publicKey: String? = null,
)

@Serializable
data class GroupDto(
    val id: Int,
    val name: String,
    val creator: GroupCreatorDto,
    @SerialName("member_count") val memberCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    val members: List<GroupMemberDto>? = null,
)
