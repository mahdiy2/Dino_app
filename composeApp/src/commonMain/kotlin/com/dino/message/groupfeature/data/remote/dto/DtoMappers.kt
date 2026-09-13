package com.dino.message.groupfeature.data.remote.dto

import com.dino.message.groupfeature.domain.model.Group
import com.dino.message.groupfeature.domain.model.GroupCreator
import com.dino.message.groupfeature.domain.model.GroupMember
import com.dino.message.groupfeature.domain.model.GroupOnlineUser

fun GroupDto.toDomain(): Group = Group(
    id = id,
    name = name,
    creator = GroupCreator(
        id = creator.id,
        username = creator.username,
        name = creator.name ?: creator.username,
        profileImageUrl = creator.profileImageUrl,
        profileImageShape = creator.profileImageShape,
        badge = creator.badge,
    ),
    roomKey = roomKey,
    isPublicRoom = isPublicRoom,
    messageTtlSeconds = messageTtlSeconds,
    memberCount = memberCount,
    hasUnread = hasUnread,
    createdAt = createdAt,
    members = members?.map {
        GroupMember(
            id = it.id,
            username = it.username,
            name = it.name ?: it.username,
            profileImageUrl = it.profileImageUrl,
            profileImageShape = it.profileImageShape,
            badge = it.badge,
            publicKey = it.publicKey,
        )
    },
)

fun GroupOnlineUserDto.toDomain(): GroupOnlineUser = GroupOnlineUser(
    userId = userId,
    username = username,
    profileImageUrl = profileImageUrl,
    profileImageShape = profileImageShape,
    badge = badge,
)
