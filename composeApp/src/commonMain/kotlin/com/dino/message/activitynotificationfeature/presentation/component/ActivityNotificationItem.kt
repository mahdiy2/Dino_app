package com.dino.message.activitynotificationfeature.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dino.message.corefeature.presentation.icons.AppIcons
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPLarge
import com.dino.message.profilefeature.presentation.component.ProfileAvatarMedia

data class ActivityNotificationUiItem(
    val actorUsername: String,
    val actorName: String?,
    val actorProfileImageUrl: String?,
    val actorBadge: String?,
    val actorProfileImageShape: String?,
    val isRead: Boolean,
    val title: String,
    val detailLines: List<NotificationDetailLine> = emptyList(),
    val subtitle: String? = null,
    val relativeTime: String,
)

data class NotificationDetailLine(
    val label: String,
    val value: String,
)

@Composable
fun ActivityNotificationItem(
    item: ActivityNotificationUiItem,
    showFollowBackButton: Boolean,
    followBackLabel: String,
    isFollowBackLoading: Boolean,
    onOpenProfile: (String) -> Unit,
    onFollowBackClick: () -> Unit,
    showReplyButton: Boolean,
    replyLabel: String,
    cancelLabel: String,
    isReplyComposerVisible: Boolean,
    replyDraft: String,
    replyPlaceholder: String,
    sendReplyLabel: String,
    isReplySending: Boolean,
    onNotificationClick: () -> Unit,
    onToggleReplyComposer: () -> Unit,
    onReplyDraftChanged: (String) -> Unit,
    onSendReply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (!item.isRead) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onNotificationClick)
            .padding(horizontal = DPExtraLarge, vertical = DPLarge),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DPLarge),
    ) {
        ProfileAvatarMedia(
            modifier = Modifier
                .size(44.dp)
                .clickable { onOpenProfile(item.actorUsername) },
            profileImageUrl = item.actorProfileImageUrl,
            badge = item.actorBadge,
            profileImageShape = item.actorProfileImageShape,
            profileVideoUrl = null,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (!item.isRead) FontWeight.SemiBold else FontWeight.Normal,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (item.detailLines.isNotEmpty()) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + expandVertically(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                    ),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    NotificationDetailSection(item.detailLines)
                }
            } else {
                item.subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = item.relativeTime,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )

            if (showFollowBackButton) {
                FilledTonalButton(
                    onClick = onFollowBackClick,
                    enabled = !isFollowBackLoading,
                ) {
                    if (isFollowBackLoading) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(followBackLabel)
                    }
                }
            }

            if (showReplyButton) {
                TextButton(onClick = onToggleReplyComposer) {
                    Icon(AppIcons.Reply, contentDescription = null)
                    Spacer(Modifier.width(2.dp))
                    Text(if (isReplyComposerVisible) cancelLabel else replyLabel)
                }
            }

            if (showReplyButton && isReplyComposerVisible) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextField(
                        modifier = Modifier.weight(1f),
                        value = replyDraft,
                        onValueChange = onReplyDraftChanged,
                        enabled = !isReplySending,
                        maxLines = 4,
                        placeholder = { Text(replyPlaceholder, style = MaterialTheme.typography.bodySmall) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                        ),
                    )
                    IconButton(
                        onClick = onSendReply,
                        enabled = !isReplySending && replyDraft.isNotBlank(),
                    ) {
                        if (isReplySending) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(AppIcons.Send, contentDescription = sendReplyLabel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationDetailSection(lines: List<NotificationDetailLine>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow,
                ),
            )
            .padding(top = 4.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        lines.forEachIndexed { index, line ->
            val rowVisibility = remember(index, line.label, line.value) {
                MutableTransitionState(false).apply { targetState = true }
            }
            AnimatedVisibility(
                visibleState = rowVisibility,
                enter = fadeIn(animationSpec = tween(180, delayMillis = index * 45)) + expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                ),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = line.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text("•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = line.value,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
