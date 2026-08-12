package com.dino.message.groupfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.groupfeature.domain.model.Group
import kotlinx.serialization.Serializable

@Serializable
data class GroupDetailResponse(
    val group: GroupDto,
) : ResponseToResultMapper<Group> {
    override fun mapResponseToResult(): Group = group.toDomain()
}
