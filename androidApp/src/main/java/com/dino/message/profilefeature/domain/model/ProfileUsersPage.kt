package com.dino.message.profilefeature.domain.model

data class ProfileUsersPage(
    val page: Int,
    val limit: Int,
    val total: Int,
    val users: List<ProfileUserSummary>
)
