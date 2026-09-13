package com.dino.message.corefeature.presentation.util

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Shape

const val PROFILE_IMAGE_SHAPE_CIRCLE = "circle"
const val PROFILE_IMAGE_SHAPE_PILL = "pill"
const val PROFILE_IMAGE_SHAPE_COOKIE_12_SIDED = "cookie_12_sided"
const val PROFILE_IMAGE_SHAPE_COOKIE_9_SIDED = "cookie_9_sided"

val PROFILE_IMAGE_SHAPE_OPTIONS: List<String> = listOf(
    PROFILE_IMAGE_SHAPE_CIRCLE,
    PROFILE_IMAGE_SHAPE_PILL,
    PROFILE_IMAGE_SHAPE_COOKIE_12_SIDED,
    PROFILE_IMAGE_SHAPE_COOKIE_9_SIDED,
)

fun hasBadgeAccess(badge: String?): Boolean = !badge.isNullOrBlank()

fun normalizeProfileImageShape(rawShape: String?): String {
    val normalized = rawShape?.trim()?.lowercase().orEmpty()
    return if (normalized in PROFILE_IMAGE_SHAPE_OPTIONS) {
        normalized
    } else {
        PROFILE_IMAGE_SHAPE_CIRCLE
    }
}

@Composable
fun resolveProfileImageShape(
    badge: String?,
    profileImageShape: String?,
): Shape {
    if (!hasBadgeAccess(badge)) return CircleShape
    return when (normalizeProfileImageShape(profileImageShape)) {
        PROFILE_IMAGE_SHAPE_PILL -> RoundedCornerShape(percent = 50)
        PROFILE_IMAGE_SHAPE_COOKIE_12_SIDED -> RoundedCornerShape(percent = 28)
        PROFILE_IMAGE_SHAPE_COOKIE_9_SIDED -> RoundedCornerShape(percent = 36)
        PROFILE_IMAGE_SHAPE_CIRCLE -> CircleShape
        else -> CircleShape
    }
}
