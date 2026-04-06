package com.dino.message.threadfeature.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dino.message.R
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.corefeature.presentation.util.rememberLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import com.dino.message.corefeature.presentation.util.getTimeAgo
import com.dino.message.threadfeature.domain.model.Post
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy

@Composable
fun ThreadPostCard(
    modifier: Modifier = Modifier,
    post: Post,
    selectedPostVote: Long = 0L,
    postError: String? = null,
    showActions: Boolean = true,
    onUpVoteClick: (() -> Unit)? = null,
    onDownVoteClick: (() -> Unit)? = null,
    onCommentsClick: (() -> Unit)? = null,
    onUserClick: (() -> Unit)? = null,
    onReportClick: (() -> Unit)? = null,
    onBlockUserClick: (() -> Unit)? = null
) {
    val commentsContentColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "comment"
    )
    var showPostMenu by remember { mutableStateOf(false) }

    PostContainer(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
                .padding(DPMedium)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DPSmall),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = if (onUserClick != null) {
                        Modifier.clickable { onUserClick() }
                    } else {
                        Modifier
                    }
                ) {
                    PostAuthorAvatar(
                        imageUrl = post.author.profile_image_url,
                        modifier = Modifier.size(30.dp)
                    )
                    Column {
                        Text(
                            text = post.author.name?.takeIf { it.isNotBlank() }
                                ?: post.author.username,
                            style = MaterialTheme.typography.labelLarge
                        )
                        if (!post.author.name.isNullOrBlank()) {
                            Text(
                                text = "@${post.author.username}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${getTimeAgo(post.created_at)} ago",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (onReportClick != null || onBlockUserClick != null) {
                        Box {
                            IconButton(
                                onClick = { showPostMenu = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "Post actions",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = showPostMenu,
                                onDismissRequest = { showPostMenu = false }
                            ) {
                                if (onReportClick != null) {
                                    DropdownMenuItem(
                                        text = { Text("Report") },
                                        onClick = {
                                            showPostMenu = false
                                            onReportClick()
                                        }
                                    )
                                }
                                if (onBlockUserClick != null) {
                                    DropdownMenuItem(
                                        text = { Text("Block User") },
                                        onClick = {
                                            showPostMenu = false
                                            onBlockUserClick()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }


            Spacer(modifier = Modifier.height(DPSmall))
            CompositionLocalProvider(LocalLayoutDirection provides rememberLayoutDirection(post.text)) {
                Text(
                    text = post.text,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (post.media.isNotEmpty()) {
                Carousel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = DPMedium),
                    items = post.media
                )
            }

            if (showActions) {
                Spacer(modifier = Modifier.height(DPMedium))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.extraLarge)
                        .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
                            shape = MaterialTheme.shapes.extraLarge
                        )
                        .padding(DPMedium),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(DPSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VoteIconButton(
                            selected = selectedPostVote == 1L,
                            onClick = { onUpVoteClick?.invoke() },
                            icon = R.drawable.ic_like_filled
                        )

                        VoteIconButton(
                            selected = selectedPostVote == (-1L),
                            onClick = { onDownVoteClick?.invoke() },
                            icon = R.drawable.ic_dislike_filled
                        )
                    }

                    IconButton(
                        onClick = { onCommentsClick?.invoke() },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = commentsContentColor
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_commets_filled),
                            contentDescription = null
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = postError != null,
                enter = fadeIn() + expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
                exit = fadeOut() + shrinkVertically()
            ) {
                postError?.let {
                    Spacer(modifier = Modifier.height(DPSmall))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            modifier = Modifier.padding(DPSmall),
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PostAuthorAvatar(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    val normalizedImageUrl = imageUrl?.takeIf { it.isNotBlank() }
    val context = LocalContext.current

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface
    ) {
        if (normalizedImageUrl == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            return@Surface
        }

        SubcomposeAsyncImage(
            model = remember(normalizedImageUrl) {
                ImageRequest.Builder(context)
                    .data(normalizedImageUrl)
                    .crossfade(true)
                    .memoryCacheKey(normalizedImageUrl)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .build()
            },
            contentDescription = "Author avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 1.5.dp
                    )
                }
            },
            error = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        )
    }
}
