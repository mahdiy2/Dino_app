package com.dino.message.threadfeature.domain.model

data class BlockedUserResult(
    val message: String,
    val blockedUsername: String,
    val created: Boolean
)
