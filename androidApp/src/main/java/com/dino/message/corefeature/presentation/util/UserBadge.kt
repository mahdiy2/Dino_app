package com.dino.message.corefeature.presentation.util

enum class UserBadgeRole {
    STAFF,
    MODERATOR,
    VERIFIED,
    POST_OF_THE_DAY,
}

fun parseUserBadgeRole(rawBadge: String?): UserBadgeRole? {
    return when (rawBadge?.trim()?.lowercase()) {
        "staff" -> UserBadgeRole.STAFF
        "moderator" -> UserBadgeRole.MODERATOR
        "verified" -> UserBadgeRole.VERIFIED
        "post_of_the_day" -> UserBadgeRole.POST_OF_THE_DAY
        else -> null
    }
}
