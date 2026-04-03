package com.dino.message.groupfeature.data.remote.dto

import com.dino.message.groupfeature.domain.model.Group
import com.dino.message.groupfeature.domain.model.GroupCreator
import com.dino.message.groupfeature.domain.model.GroupMember

fun GroupDto.toDomain(): Group = Group(
    id = id,
    name = name,
    creator = GroupCreator(
        id = creator.id,
        username = creator.username,
        name = creator.name ?: creator.username,
        profileImageUrl = creator.profileImageUrl,
    ),
    memberCount = memberCount,
    createdAt = createdAt,
    members = members?.map {
        GroupMember(
            id = it.id,
            username = it.username,
            name = it.name ?: it.username,
            profileImageUrl = it.profileImageUrl,
            publicKey = it.publicKey,
        )
    },
)
