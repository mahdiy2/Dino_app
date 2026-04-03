package com.dino.message.groupfeature.domain.model

data class MutualFollower(
    val id: Int,
    val username: String,
    val name: String,
    val profileImageUrl: String? = null,
)

data class MutualFollowersPage(
    val page: Int,
    val limit: Int,
    val total: Int,
    val users: List<MutualFollower>,
)
