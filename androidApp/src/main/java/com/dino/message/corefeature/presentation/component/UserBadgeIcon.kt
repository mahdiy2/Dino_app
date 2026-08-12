package com.dino.message.corefeature.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Surface
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dino.message.corefeature.presentation.util.UserBadgeRole
import com.dino.message.corefeature.presentation.util.parseUserBadgeRole
import com.dino.message.corefeature.presentation.icons.AppIcons

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UserBadgeIcon(
    badge: String?,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
) {
    val role = parseUserBadgeRole(badge) ?: return
    val isDark = isSystemInDarkTheme()
    val resolvedSize = size.coerceAtLeast(18.dp)
    val backgroundColor = when (role) {
        UserBadgeRole.STAFF -> Color(0xFF111111)
        UserBadgeRole.MODERATOR -> Color(0xFF1E8E3E)
        UserBadgeRole.VERIFIED -> Color(0xFF2563EB)
        UserBadgeRole.POST_OF_THE_DAY -> Color(0xFFE07A00)
    }

    val shape = when (role) {
        UserBadgeRole.STAFF -> MaterialShapes.Cookie12Sided.toShape()
        UserBadgeRole.MODERATOR, UserBadgeRole.VERIFIED, UserBadgeRole.POST_OF_THE_DAY -> MaterialShapes.Pill.toShape()
    }
    val border = when (role) {
        UserBadgeRole.STAFF -> if (isDark) BorderStroke(0.5.dp, Color.White.copy(alpha = 0.9f)) else BorderStroke(0.5.dp, Color.White.copy(alpha = 0.45f))
        UserBadgeRole.MODERATOR -> BorderStroke(0.4.dp, Color.White.copy(alpha = 0.35f))
        UserBadgeRole.VERIFIED -> BorderStroke(0.4.dp, Color.White.copy(alpha = 0.35f))
        UserBadgeRole.POST_OF_THE_DAY -> BorderStroke(0.4.dp, Color.White.copy(alpha = 0.35f))
    }
    val iconSize = (resolvedSize.value * 0.58f).dp

    Surface(
        modifier = modifier.size(resolvedSize),
        shape = shape,
        color = backgroundColor,
        border = border,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.Check,
                contentDescription = role.name.lowercase(),
                tint = Color.White,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
