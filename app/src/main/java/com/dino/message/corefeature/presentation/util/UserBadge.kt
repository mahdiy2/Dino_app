package com.dino.message.corefeature.presentation.util

enum class UserBadgeRole {
    STAFF,
    MODERATOR,
    VERIFIED,
}

fun parseUserBadgeRole(rawBadge: String?): UserBadgeRole? {
    return when (rawBadge?.trim()?.lowercase()) {
        "staff" -> UserBadgeRole.STAFF
        "moderator" -> UserBadgeRole.MODERATOR
        "verified" -> UserBadgeRole.VERIFIED
        else -> null
    }
}
