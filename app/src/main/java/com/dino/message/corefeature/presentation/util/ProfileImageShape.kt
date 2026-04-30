package com.dino.message.corefeature.presentation.util

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun resolveProfileImageShape(
    badge: String?,
    profileImageShape: String?,
): Shape {
    if (!hasBadgeAccess(badge)) return MaterialShapes.Circle.toShape()
    return when (normalizeProfileImageShape(profileImageShape)) {
        PROFILE_IMAGE_SHAPE_PILL -> MaterialShapes.Pill.toShape()
        PROFILE_IMAGE_SHAPE_COOKIE_12_SIDED -> MaterialShapes.Cookie12Sided.toShape()
        PROFILE_IMAGE_SHAPE_COOKIE_9_SIDED -> MaterialShapes.Cookie9Sided.toShape()
        PROFILE_IMAGE_SHAPE_CIRCLE -> MaterialShapes.Circle.toShape()
        else -> MaterialShapes.Circle.toShape()
    }
}
