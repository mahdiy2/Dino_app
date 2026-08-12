package com.dino.message.groupfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.groupfeature.domain.model.GroupOnlineUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GroupOnlineUserDto(
    @SerialName("user_id") val userId: String,
    val username: String,
    @SerialName("profile_image_url") val profileImageUrl: String? = null,
    @SerialName("profile_image_shape") val profileImageShape: String? = null,
    val badge: String? = null,
)

@Serializable
data class GroupOnlineUsersResponse(
    @SerialName("group_id") val groupId: Int,
    @SerialName("online_users") val onlineUsers: List<GroupOnlineUserDto> = emptyList(),
) : ResponseToResultMapper<List<GroupOnlineUser>> {
    override fun mapResponseToResult(): List<GroupOnlineUser> =
        onlineUsers.map { it.toDomain() }
}
