package com.dino.message.groupfeature.domain.model

data class GroupCreator(
    val id: Int,
    val username: String,
    val name: String,
    val profileImageUrl: String? = null,
)

data class GroupMember(
    val id: Int,
    val username: String,
    val name: String,
    val profileImageUrl: String? = null,
    val publicKey: String? = null,
)

data class Group(
    val id: Int,
    val name: String,
    val creator: GroupCreator,
    val memberCount: Int = 0,
    val createdAt: String = "",
    val members: List<GroupMember>? = null,
)
