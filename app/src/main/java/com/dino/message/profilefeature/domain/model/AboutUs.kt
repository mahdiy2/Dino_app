package com.dino.message.profilefeature.domain.model

data class AboutUs(
    val description: String,
    val links: List<AboutUsLink>,
    val teamMembers: List<AboutUsTeamMember>,
)

data class AboutUsLink(
    val type: String,
    val title: String,
    val url: String,
    val isDisabled: Boolean,
)

data class AboutUsTeamMember(
    val userId: Long,
    val username: String,
    val name: String,
    val profileImageUrl: String?,
    val role: String,
)
