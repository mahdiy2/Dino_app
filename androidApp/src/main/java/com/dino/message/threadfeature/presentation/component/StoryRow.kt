package com.dino.message.threadfeature.presentation.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dino.message.corefeature.presentation.util.resolveProfileImageShape
import com.dino.message.profilefeature.domain.model.Profile
import com.dino.message.profilefeature.presentation.component.ProfileAvatarMedia
import com.dino.message.threadfeature.domain.model.StoryUserFeedItem
import com.dino.message.corefeature.presentation.icons.AppIcons

@Composable
fun StoryRow(
    items: List<StoryUserFeedItem>,
    viewerProfile: Profile?,
    hasActiveYourStory: Boolean,
    onViewYourStory: () -> Unit,
    onCreateNewStory: () -> Unit,
    onStoryClick: (StoryUserFeedItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            item(key = "your_story") {
                StoryCircleItem(
                    title = "Your Story",
                    imageUrl = viewerProfile?.profile_image_url,
                    badge = viewerProfile?.badge,
                    profileImageShape = viewerProfile?.profile_image_shape,
                    hasUnseen = false,
                    hasAddBadge = true,
                    showYourStoryMenu = hasActiveYourStory,
                    onClick = onCreateNewStory,
                    onViewYourStory = onViewYourStory,
                    onCreateNewStory = onCreateNewStory
                )
            }

            items(items = items, key = { "story_user_${it.user_id}" }) { item ->
                StoryCircleItem(
                    title = item.name?.takeIf { name -> name.isNotBlank() } ?: item.username,
                    imageUrl = item.avatar,
                    badge = item.badge,
                    profileImageShape = item.profile_image_shape,
                    hasUnseen = item.has_unseen_story,
                    hasAddBadge = false,
                    showYourStoryMenu = false,
                    onClick = { onStoryClick(item) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun StoryCircleItem(
    title: String,
    imageUrl: String?,
    badge: String?,
    profileImageShape: String?,
    hasUnseen: Boolean,
    hasAddBadge: Boolean,
    showYourStoryMenu: Boolean,
    onClick: () -> Unit,
    onViewYourStory: (() -> Unit)? = null,
    onCreateNewStory: (() -> Unit)? = null
) {
    val avatarShape = resolveProfileImageShape(
        badge = badge,
        profileImageShape = profileImageShape
    )
    var isMenuExpanded by remember { mutableStateOf(false) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .size(width = 78.dp, height = 96.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable {
                if (hasAddBadge && showYourStoryMenu) {
                    isMenuExpanded = true
                } else {
                    onClick()
                }
            }
            .padding(top = 2.dp)
    ) {
        Surface(
            shape = avatarShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 2.5.dp,
                color = if (hasUnseen) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                ProfileAvatarMedia(
                    modifier = Modifier.size(58.dp),
                    profileImageUrl = imageUrl,
                    badge = badge,
                    profileImageShape = profileImageShape,
                    profileVideoUrl = null
                )
            }
        }

        if (hasAddBadge) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "New",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = { isMenuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("View Story") },
                    onClick = {
                        isMenuExpanded = false
                        onViewYourStory?.invoke()
                    }
                )
                DropdownMenuItem(
                    text = { Text("New Story") },
                    onClick = {
                        isMenuExpanded = false
                        onCreateNewStory?.invoke()
                    }
                )
            }
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
