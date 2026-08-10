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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dino.message.corefeature.presentation.platform.PlatformAsyncImage
import com.dino.message.corefeature.presentation.platform.PlatformImageRequestOptions
import com.dino.message.corefeature.presentation.platform.PreloadPlatformImage
import com.dino.message.corefeature.presentation.platform.PlatformVideoPlayer
import com.dino.message.corefeature.presentation.platform.PlatformVideoResizeMode
import com.dino.message.corefeature.presentation.platform.isPlatformImageError
import com.dino.message.corefeature.presentation.platform.isPlatformImageLoading
import com.dino.message.corefeature.presentation.platform.rememberPlatformImageRequest
import com.dino.message.corefeature.presentation.util.resolveProfileImageShape
import com.dino.message.corefeature.presentation.icons.AppIcons

@Composable
fun ProfileAvatarMedia(
    profileImageUrl: String?,
    profileVideoUrl: String?,
    badge: String? = null,
    profileImageShape: String? = null,
    modifier: Modifier = Modifier,
    autoPlayVideo: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val normalizedImageUrl = profileImageUrl?.takeIf { it.isNotBlank() }
    val normalizedVideoUrl = profileVideoUrl?.takeIf { it.isNotBlank() }
    var hasVideoFinished by remember(normalizedVideoUrl, normalizedImageUrl, autoPlayVideo) {
        mutableStateOf(!autoPlayVideo || normalizedVideoUrl == null)
    }
    val shouldShowVideo = normalizedVideoUrl != null && !hasVideoFinished
    val avatarShape = resolveProfileImageShape(
        badge = badge,
        profileImageShape = profileImageShape
    )

    PreloadProfileImage(
        imageUrl = normalizedImageUrl,
        enabled = shouldShowVideo
    )

    Surface(
        modifier = if (onClick != null) {
            modifier
                .clip(avatarShape)
                .clickable(onClick = onClick)
        } else {
            modifier
                .clip(avatarShape)
        },
        shape = avatarShape,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        if (shouldShowVideo) {
            ProfileVideoAutoPlayer(
                url = normalizedVideoUrl!!,
                modifier = Modifier.fillMaxSize(),
                onFinished = { hasVideoFinished = true }
            )
        } else {
            ProfileImageAvatar(
                imageUrl = normalizedImageUrl,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun PreloadProfileImage(
    imageUrl: String?,
    enabled: Boolean
) {
    if (!enabled || imageUrl == null) return
    PreloadPlatformImage(model = imageUrl)
}

@Composable
private fun ProfileImageAvatar(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    if (imageUrl == null) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var isLoading by remember(imageUrl) { mutableStateOf(true) }
    var isError by remember(imageUrl) { mutableStateOf(false) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        PlatformAsyncImage(
            model = rememberPlatformImageRequest(
                data = imageUrl,
                options = PlatformImageRequestOptions(
                    memoryCacheKey = imageUrl,
                    placeholderMemoryCacheKey = imageUrl,
                    enableDiskCache = true,
                    enableMemoryCache = true
                )
            ),
            contentDescription = "Profile image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            onState = { state ->
                isLoading = state.isPlatformImageLoading
                isError = state.isPlatformImageError
            }
        )

        if (isError) {
            Icon(
                imageVector = AppIcons.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun ProfileVideoAutoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    onFinished: () -> Unit
) {
    var isBuffering by remember(url) { mutableStateOf(true) }

    Box(modifier = modifier) {
        PlatformVideoPlayer(
            url = url,
            modifier = Modifier.fillMaxSize(),
            useController = false,
            resizeMode = PlatformVideoResizeMode.Zoom,
            onBufferingChanged = { isBuffering = it },
            onCompleted = onFinished,
            onPlaybackError = { onFinished() }
        )

        if (isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}
