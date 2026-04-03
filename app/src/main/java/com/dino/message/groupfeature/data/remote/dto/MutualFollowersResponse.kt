package com.dino.message.groupfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.groupfeature.domain.model.MutualFollower
import com.dino.message.groupfeature.domain.model.MutualFollowersPage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MutualFollowerDto(
    val id: Int,
    val username: String,
    val name: String? = null,
    @SerialName("profile_image_url") val profileImageUrl: String? = null,
)

@Serializable
data class MutualFollowersResponse(
    val page: Int = 1,
    val limit: Int = 50,
    val total: Int = 0,
    val users: List<MutualFollowerDto>,
) : ResponseToResultMapper<MutualFollowersPage> {
    override fun mapResponseToResult(): MutualFollowersPage = MutualFollowersPage(
        page = page,
        limit = limit,
        total = total,
        users = users.map {
            MutualFollower(
                id = it.id,
                username = it.username,
                name = it.name ?: it.username,
                profileImageUrl = it.profileImageUrl,
            )
        },
    )
}
