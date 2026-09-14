package com.dino.message.profilefeature.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.dino.message.corefeature.presentation.icons.AppIcons
import com.dino.message.corefeature.presentation.platform.LocalPlatformMediaRenderer
import com.dino.message.corefeature.presentation.platform.PlatformImageLoadState
import com.dino.message.corefeature.presentation.platform.PlatformImageOptions
import com.dino.message.corefeature.presentation.util.resolveProfileImageShape

@Composable
fun ProfileAvatarMedia(
    profileImageUrl: String?,
    profileVideoUrl: String?,
    badge: String? = null,
    profileImageShape: String? = null,
    modifier: Modifier = Modifier,
    autoPlayVideo: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val renderer = LocalPlatformMediaRenderer.current
    val normalizedImageUrl = profileImageUrl?.takeIf { it.isNotBlank() }
    val normalizedVideoUrl = profileVideoUrl?.takeIf { it.isNotBlank() }
    var hasVideoFinished by remember(normalizedVideoUrl, normalizedImageUrl, autoPlayVideo) {
        mutableStateOf(!autoPlayVideo || normalizedVideoUrl == null)
    }
    val shouldShowVideo = normalizedVideoUrl != null && !hasVideoFinished
    val avatarShape = resolveProfileImageShape(badge, profileImageShape)

    if (shouldShowVideo && normalizedImageUrl != null) {
        renderer.preloadImage(normalizedImageUrl)
    }

    Surface(
        modifier = if (onClick != null) modifier.clip(avatarShape).clickable(onClick = onClick)
        else modifier.clip(avatarShape),
        shape = avatarShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        if (shouldShowVideo) {
            var isBuffering by remember(normalizedVideoUrl) { mutableStateOf(true) }
            Box(modifier = Modifier.fillMaxSize()) {
                renderer.video(
                    normalizedVideoUrl!!,
                    Modifier.fillMaxSize(),
                    true,
                    { isBuffering = it },
                    { hasVideoFinished = true },
                    { hasVideoFinished = true },
                )
                if (isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center).size(18.dp),
                        strokeWidth = 2.dp,
                    )
                }
            }
        } else {
            ProfileImageAvatar(normalizedImageUrl, Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ProfileImageAvatar(imageUrl: String?, modifier: Modifier) {
    if (imageUrl == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Icon(AppIcons.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val renderer = LocalPlatformMediaRenderer.current
    var state by remember(imageUrl) { mutableStateOf(PlatformImageLoadState.Loading) }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        renderer.image(
            imageUrl,
            null,
            Modifier.fillMaxSize(),
            ContentScale.Crop,
            PlatformImageOptions(
                memoryCacheKey = imageUrl,
                placeholderMemoryCacheKey = imageUrl,
                enableDiskCache = true,
                enableMemoryCache = true,
            ),
            { state = it },
        )
        when (state) {
            PlatformImageLoadState.Loading -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            PlatformImageLoadState.Error -> Icon(AppIcons.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            PlatformImageLoadState.Success -> Unit
        }
    }
}
