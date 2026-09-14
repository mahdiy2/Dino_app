package com.dino.message.searchfeature.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dino.message.corefeature.presentation.component.UserBadgeIcon
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.profilefeature.presentation.component.ProfileAvatarMedia
import com.dino.message.searchfeature.domain.model.SearchUser

@Composable
fun SearchUserItem(user: SearchUser, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = DPMedium, vertical = DPMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DPMedium),
    ) {
        ProfileAvatarMedia(
            modifier = Modifier.size(44.dp),
            profileImageUrl = user.profile_image_url,
            profileVideoUrl = null,
            badge = user.badge,
            profileImageShape = user.profile_image_shape,
        )
        Column(verticalArrangement = Arrangement.spacedBy(DPSmall)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(user.username, style = MaterialTheme.typography.titleSmall)
                UserBadgeIcon(badge = user.badge, size = 16.dp)
            }
            user.name?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
