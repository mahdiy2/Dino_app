package com.dino.message.threadfeature.presentation.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dino.message.R
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.profilefeature.presentation.component.ProfileAvatarMedia
import com.dino.message.corefeature.presentation.icons.AppIcons

private val PILL_HEIGHT = 56.dp
private val CENTER_BUTTON_SIZE = 44.dp
private val ICON_SIZE = 22.dp
private val PILL_CORNER = 28.dp
private val PROFILE_AVATAR_SIZE = 30.dp

@Composable
fun FloatingDockNavigation(
    selectedIndex: Int,
    unreadNotificationCount: Int,
    profileBadge: String?,
    profileImageUrl: String?,
    profileImageShape: String?,
    profileVideoUrl: String?,
    onChatClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(bottom = DPMedium),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(PILL_CORNER),
                    clip = false,
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
                .height(PILL_HEIGHT),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            shape = RoundedCornerShape(PILL_CORNER),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Row(
            modifier = Modifier
//                .shadow(
//                    elevation = 12.dp,
//                    shape = RoundedCornerShape(PILL_CORNER),
//                    clip = false,
//                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
//                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
//                )
//                .clip(RoundedCornerShape(PILL_CORNER))
//                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
//                .height(PILL_HEIGHT)
                .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            )
            {
                DockItem(
                    painter = painterResource(R.drawable.ic_message_filled),
                    contentDescription = "Chat",
                    selected = selectedIndex == 0,
                    onClick = onChatClick
                )

                DockItemWithBadge(
                    painter = painterResource(R.drawable.ic_notifications),
                    contentDescription = "Notifications",
                    selected = selectedIndex == 1,
                    badgeCount = unreadNotificationCount,
                    onClick = onNotificationsClick
                )

                CenterCreateButton(onClick = onFabClick)

                DockItem(
                    imageVector = AppIcons.LibraryMusic,
                    contentDescription = "Playlist",
                    selected = selectedIndex == 2,
                    onClick = onPlaylistClick
                )

                DockProfileItem(
                    badge = profileBadge,
                    profileImageUrl = profileImageUrl,
                    profileImageShape = profileImageShape,
                    profileVideoUrl = profileVideoUrl,
                    onClick = onProfileClick
                )
            }
        }
    }
}

@Composable
private fun DockProfileItem(
    badge: String?,
    profileImageUrl: String?,
    profileImageShape: String?,
    profileVideoUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "profileItemScale"
    )

    Box(
        modifier = modifier
            .widthIn(min = 48.dp)
            .height(PILL_HEIGHT)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        ProfileAvatarMedia(
            modifier = Modifier
                .size(PROFILE_AVATAR_SIZE)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            profileImageUrl = profileImageUrl,
            badge = badge,
            profileImageShape = profileImageShape,
            profileVideoUrl = profileVideoUrl,
            autoPlayVideo = true
        )
    }
}

@Composable
private fun CenterCreateButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "centerBtnScale"
    )

    val iconRotation by animateFloatAsState(
        targetValue = if (isPressed) 90f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "centerBtnRotation"
    )

    Box(
        modifier = modifier
            .size(CENTER_BUTTON_SIZE)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = AppIcons.Add,
            contentDescription = "Create post",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer { rotationZ = iconRotation }
        )
    }
}

@Composable
private fun DockItem(
    imageVector: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val tint by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dockItemTint"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.82f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dockItemScale"
    )

    Box(
        modifier = modifier
            .widthIn(min = 48.dp)
            .height(PILL_HEIGHT)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(ICON_SIZE)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            tint = tint
        )
    }
}

@Composable
private fun DockItemWithBadge(
    painter: Painter,
    contentDescription: String,
    selected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val tint by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dockItemBadgeTint"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.82f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dockItemBadgeScale"
    )

    Box(
        modifier = modifier
            .widthIn(min = 48.dp)
            .height(PILL_HEIGHT)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        BadgedBox(
            badge = {
                if (badgeCount > 0) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ) {
                        Text(
                            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        ) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
                modifier = Modifier
                    .size(ICON_SIZE)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                tint = tint
            )
        }
    }
}

@Composable
private fun DockItem(
    painter: Painter,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val tint by animateColorAsState(
        targetValue = if (selected)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "dockItemTint"
    )

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.82f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dockItemScale"
    )

    Box(
        modifier = modifier
            .widthIn(min = 48.dp)
            .height(PILL_HEIGHT)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier
                .size(ICON_SIZE)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            tint = tint
        )
    }
}
