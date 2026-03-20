package com.dino.message.mainpage.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.corefeature.presentation.util.getTimeAgo
import com.dino.message.profilefeature.presentation.component.ProfileAvatarMedia

@Composable
fun MessageListItems(
    modifier: Modifier = Modifier,
    displayName: String,
    time: String,
    newMessage: Boolean = false,
    profileImageUrl: String? = null,
    profileVideoUrl: String? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(MaterialTheme.shapes.extraLarge)
                .clickable(onClick = onClick)
                .fillMaxWidth()
                .padding(horizontal = DPMedium, vertical = DPMedium)
        ) {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileAvatarMedia(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(MaterialTheme.shapes.medium),
                    profileImageUrl = profileImageUrl,
                    profileVideoUrl = profileVideoUrl
                )

                Spacer(Modifier.size(DPMedium))

                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    AnimatedVisibility(newMessage) {
                        Column {
                            Spacer(Modifier.size(DPMedium))
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.size(14.dp),
                                    shape = CircleShape
                                ) {
                                }
                                Spacer(Modifier.size(DPSmall))
                                Text(
                                    text = "New message"
                                )
                            }
                        }
                    }
                }
            }

            if (time.isNotBlank()) {
                val relative = getTimeAgo(time)
                Text(
                    text = if (relative == "now") relative else "$relative ago",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
