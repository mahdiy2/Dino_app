package com.dino.message.profilefeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.profilefeature.domain.model.AboutUs
import com.dino.message.profilefeature.domain.model.AboutUsLink
import com.dino.message.profilefeature.domain.model.AboutUsTeamMember
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AboutUsResponse(
    val description: String = "",
    val links: List<AboutUsLinkResponse> = emptyList(),
    @SerialName("team_members")
    val teamMembers: List<AboutUsTeamMemberResponse> = emptyList(),
) : ResponseToResultMapper<AboutUs> {
    override fun mapResponseToResult(): AboutUs = AboutUs(
        description = description,
        links = links.map {
            AboutUsLink(
                type = it.type,
                title = it.title,
                url = it.url,
                isDisabled = it.isDisabled,
            )
        },
        teamMembers = teamMembers.map {
            AboutUsTeamMember(
                userId = it.userId,
                username = it.username,
                name = it.name,
                profileImageUrl = it.profileImageUrl,
                role = it.role,
            )
        },
    )
}

@Serializable
data class AboutUsLinkResponse(
    val type: String = "",
    val title: String = "",
    val url: String = "",
    @SerialName("is_disabled")
    val isDisabled: Boolean = false,
)

@Serializable
data class AboutUsTeamMemberResponse(
    @SerialName("user_id")
    val userId: Long = 0L,
    val username: String = "",
    val name: String = "",
    @SerialName("profile_image_url")
    val profileImageUrl: String? = null,
    val role: String = "",
)
