package com.dino.message.groupfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.groupfeature.domain.model.JoinPublicRoomResult
import com.dino.message.groupfeature.domain.model.PublicRoomStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PublicRoomStatusResponse(
    val group: GroupDto,
    @SerialName("is_member") val isMember: Boolean,
) : ResponseToResultMapper<PublicRoomStatus> {
    override fun mapResponseToResult(): PublicRoomStatus = PublicRoomStatus(
        group = group.toDomain(),
        isMember = isMember,
    )
}

@Serializable
data class JoinPublicRoomResponse(
    val group: GroupDto,
    @SerialName("joined_now") val joinedNow: Boolean,
) : ResponseToResultMapper<JoinPublicRoomResult> {
    override fun mapResponseToResult(): JoinPublicRoomResult = JoinPublicRoomResult(
        group = group.toDomain(),
        joinedNow = joinedNow,
    )
}
