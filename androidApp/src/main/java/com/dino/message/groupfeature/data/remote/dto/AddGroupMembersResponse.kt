package com.dino.message.groupfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.groupfeature.domain.model.AddGroupMembersResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddGroupMembersResponse(
    val added: List<String> = emptyList(),
    @SerialName("already_members") val alreadyMembers: List<String> = emptyList(),
) : ResponseToResultMapper<AddGroupMembersResult> {
    override fun mapResponseToResult(): AddGroupMembersResult = AddGroupMembersResult(
        added = added,
        alreadyMembers = alreadyMembers,
    )
}
