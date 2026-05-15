package com.dino.message.profilefeature.presentation.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.SubcomposeAsyncImage
import com.dino.message.R
import com.dino.message.corefeature.presentation.component.UserBadgeIcon
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.corefeature.presentation.util.RoundedPolygonShape
import com.dino.message.corefeature.presentation.util.backIconModifier
import com.dino.message.profilefeature.domain.model.ProfileUserSummary
import com.dino.message.profilefeature.presentation.component.ProfileAvatarMedia
import com.dino.message.profilefeature.presentation.viewmodel.ProfileFollowListViewModel
import com.dino.message.threadfeature.presentation.component.PostContainer
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>
@Composable
fun ProfileFollowListScreen(
    username: String,
    showFollowers: Boolean,
) {
    val viewModel: ProfileFollowListViewModel = hiltViewModel()
    val listState = rememberLazyListState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()
    val error by viewModel.error.collectAsState()
    val title by viewModel.title.collectAsState()
    val isOwnProfile by viewModel.isOwnProfile.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()

    LaunchedEffect(username, showFollowers) {
        viewModel.initialize(
            username = username,
            showFollowers = showFollowers,
        )
    }

    LaunchedEffect(listState, hasMore, isLoadingMore, isLoading) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { lastVisibleIndex ->
                val shouldLoadMore = hasMore && !isLoadingMore && !isLoading
                val triggerIndex = (viewModel.users.lastIndex - 3).coerceAtLeast(0)
                if (shouldLoadMore && viewModel.users.isNotEmpty() && lastVisibleIndex >= triggerIndex) {
                    viewModel.loadNextPage()
                }
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    IconButton(onClick = viewModel::navigateUp) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBackIosNew,
                            contentDescription = stringResource(R.string.back),
                            modifier = backIconModifier(),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            FollowListAnimatedBackdrop(modifier = Modifier.fillMaxSize())

            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = DPExtraLarge,
                        vertical = DPLarge,
                    ),
                    verticalArrangement = Arrangement.spacedBy(DPLarge),
                ) {
                    item("hero") {
                        AnimatedSection(delayMillis = 40) {
                            FollowListHeroCard(
                                showFollowers = showFollowers,
                                username = username,
                                usersCount = totalCount,
                            )
                        }
                    }

                    if (error != null) {
                        item("error") {
                            AnimatedSection(delayMillis = 70) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = MaterialTheme.shapes.large,
                                ) {
                                    Text(
                                        modifier = Modifier.padding(DPLarge),
                                        text = error.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            }
                        }
                    }

                    if (isLoading && viewModel.users.isEmpty()) {
                        item("loading") {
                            AnimatedSection(delayMillis = 90) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = DPLarge),
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    } else if (viewModel.users.isEmpty()) {
                        item("empty") {
                            AnimatedSection(delayMillis = 90) {
                                Surface(
                                    shape = MaterialTheme.shapes.extraLarge,
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                ) {
                                    Text(
                                        modifier = Modifier.padding(DPLarge),
                                        text = if (showFollowers) {
                                            stringResource(R.string.no_followers_found)
                                        } else {
                                            stringResource(R.string.no_following_found)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    } else {
                        item("users_header") {
                            AnimatedSection(delayMillis = 90) {
                                FollowListSectionHeader(
                                    title = if (showFollowers) {
                                        stringResource(R.string.followers)
                                    } else {
                                        stringResource(R.string.following)
                                    },
                                )
                            }
                        }

                        itemsIndexed(
                            items = viewModel.users,
                            key = { _, user -> user.id },
                        ) { index, user ->
                            AnimatedSection(delayMillis = 120 + minOf(index * 30, 220)) {
                                FollowUserCard(
                                    user = user,
                                    onClick = { viewModel.openUserProfile(user.username) },
                                    showActionButton = isOwnProfile,
                                    actionLabel = if (showFollowers) {
                                        stringResource(R.string.remove_follower)
                                    } else {
                                        stringResource(R.string.unfollow)
                                    },
                                    isActionLoading = viewModel.isActionLoading(user.username),
                                    onActionClick = { viewModel.onUserAction(user) },
                                )
                            }
                        }

                        if (isLoadingMore) {
                            item("load_more") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = DPMedium),
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowListAnimatedBackdrop(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "follow_list_background")
    val firstX by transition.animateFloat(
        initialValue = -0.24f,
        targetValue = 0.52f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10_500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "follow_shape_hex_x",
    )
    val secondX by transition.animateFloat(
        initialValue = 0.34f,
        targetValue = -0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12_500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "follow_shape_pent_x",
    )
    val thirdY by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = -0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9_400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "follow_shape_octo_y",
    )
    val firstRotation by transition.animateFloat(
        initialValue = -16f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8_500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "follow_shape_hex_rotation",
    )
    val secondRotation by transition.animateFloat(
        initialValue = 10f,
        targetValue = -14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9_700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "follow_shape_pent_rotation",
    )
    val thirdRotation by transition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7_900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "follow_shape_octo_rotation",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8_600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "follow_shape_pulse",
    )

    val hexShape = remember {
        RoundedPolygonShape(
            RoundedPolygon(
                numVertices = 6,
                rounding = CornerRounding(0.22f),
            ),
        )
    }
    val pentShape = remember {
        RoundedPolygonShape(
            RoundedPolygon(
                numVertices = 5,
                rounding = CornerRounding(0.24f),
            ),
        )
    }
    val octShape = remember {
        RoundedPolygonShape(
            RoundedPolygon(
                numVertices = 8,
                rounding = CornerRounding(0.18f),
            ),
        )
    }

    BoxWithConstraints(modifier = modifier) {
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = maxWidth * firstX,
                    y = maxHeight * 0.05f,
                )
                .size(
                    width = maxWidth * 0.84f,
                    height = 142.dp,
                )
                .graphicsLayer {
                    rotationZ = firstRotation
                    scaleX = pulse
                    scaleY = pulse
                },
            shape = hexShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        ) {}

        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(
                    x = maxWidth * secondX,
                    y = -(maxHeight * 0.07f),
                )
                .size(
                    width = maxWidth * 0.66f,
                    height = 116.dp,
                )
                .graphicsLayer {
                    rotationZ = secondRotation
                },
            shape = pentShape,
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.11f),
        ) {}

        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(
                    x = maxWidth * 0.04f,
                    y = maxHeight * thirdY,
                )
                .size(
                    width = maxWidth * 0.46f,
                    height = 102.dp,
                )
                .graphicsLayer {
                    rotationZ = thirdRotation
                },
            shape = octShape,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.09f),
        ) {}
    }
}

@Composable
private fun AnimatedSection(
    delayMillis: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 420),
        label = "follow_section_alpha",
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 18f,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "follow_section_offset",
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            translationY = offsetY
        },
    ) {
        content()
    }
}

@Composable
private fun FollowListHeroCard(
    showFollowers: Boolean,
    username: String,
    usersCount: Int,
) {
    val title = if (showFollowers) {
        stringResource(R.string.followers)
    } else {
        stringResource(R.string.following)
    }

    PostContainer(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.76f),
                        ),
                    ),
                )
                .padding(horizontal = 20.dp, vertical = 22.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(DPMedium)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DPMedium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                    ) {
                        Icon(
                            modifier = Modifier.padding(DPMedium),
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }

                Text(
                    text = if (showFollowers) {
                        stringResource(R.string.followers_list_subtitle, username)
                    } else {
                        stringResource(R.string.following_list_subtitle, username)
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = DPLarge, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(DPMedium),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                        )
                        Text(
                            text = "$usersCount",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowListSectionHeader(title: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(DPMedium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Icon(
                modifier = Modifier.padding(7.dp),
                imageVector = Icons.Rounded.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun FollowUserCard(
    user: ProfileUserSummary,
    onClick: () -> Unit,
    showActionButton: Boolean,
    actionLabel: String,
    isActionLoading: Boolean,
    onActionClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(),
        label = "follow_user_card_scale",
    )

    PostContainer(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (pressed) 6.dp else 10.dp,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DPLarge, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DPMedium),
        ) {
            ProfileAvatarMedia(
                modifier = Modifier.size(50.dp),
                profileImageUrl = user.profile_image_url,
                profileVideoUrl = null,
                badge = user.badge,
                profileImageShape = user.profile_image_shape
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DPSmall),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = user.name?.takeIf { it.isNotBlank() } ?: user.username,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    UserBadgeIcon(
                        badge = user.badge,
                        size = 17.dp
                    )
                }
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (showActionButton) {
                FilledTonalButton(
                    onClick = onActionClick,
                    enabled = !isActionLoading,
                ) {
                    if (isActionLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(text = actionLabel)
                    }
                }
            }
        }
    }
}
