package com.dino.message.groupfeature.domain.model

data class GroupOnlineUser(
    val userId: String,
    val username: String,
    val profileImageUrl: String? = null,
    val profileImageShape: String? = null,
    val badge: String? = null,
)
