package com.dino.message.groupfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.groupfeature.domain.model.Group
import kotlinx.serialization.Serializable

@Serializable
data class ListGroupsResponse(
    val groups: List<GroupDto>,
) : ResponseToResultMapper<List<Group>> {
    override fun mapResponseToResult(): List<Group> = groups.map { it.toDomain() }
}
