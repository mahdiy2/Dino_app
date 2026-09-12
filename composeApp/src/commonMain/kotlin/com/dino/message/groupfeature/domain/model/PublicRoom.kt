package com.dino.message.groupfeature.domain.model

data class PublicRoomStatus(
    val group: Group,
    val isMember: Boolean,
)

data class JoinPublicRoomResult(
    val group: Group,
    val joinedNow: Boolean,
)
