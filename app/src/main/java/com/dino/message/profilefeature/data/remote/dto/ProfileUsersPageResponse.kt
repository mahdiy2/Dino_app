package com.dino.message.profilefeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.profilefeature.domain.model.ProfileUserSummary
import com.dino.message.profilefeature.domain.model.ProfileUsersPage
import kotlinx.serialization.Serializable

@Serializable
data class ProfileUsersPageResponse(
    val page: Int,
    val limit: Int,
    val total: Int,
    val users: List<ProfileUserSummary> = emptyList()
) : ResponseToResultMapper<ProfileUsersPage> {
    override fun mapResponseToResult(): ProfileUsersPage = ProfileUsersPage(
        page = page,
        limit = limit,
        total = total,
        users = users
    )
}
