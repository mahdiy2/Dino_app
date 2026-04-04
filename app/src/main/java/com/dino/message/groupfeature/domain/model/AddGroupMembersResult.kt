package com.dino.message.groupfeature.domain.model

data class AddGroupMembersResult(
    val added: List<String>,
    val alreadyMembers: List<String>,
)
