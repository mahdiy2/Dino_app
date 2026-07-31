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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.dino.message.R
import com.dino.message.corefeature.presentation.component.UserBadgeIcon
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.corefeature.presentation.util.backIconModifier
import com.dino.message.profilefeature.domain.model.ReferralSummary
import com.dino.message.profilefeature.presentation.viewmodel.ReferralViewModel
import com.dino.message.threadfeature.presentation.component.PostContainer
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralScreen() {
    val viewModel: ReferralViewModel = koinViewModel()
    val summary by viewModel.summary.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.referral_program_title)) },
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
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        ) {
            ReferralAnimatedBackdrop(modifier = Modifier.fillMaxSize())

            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                error != null -> {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(DPExtraLarge),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(
                            text = error.orEmpty(),
                            modifier = Modifier.padding(DPMedium),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }

                summary != null -> {
                    ReferralLoadedContent(
                        modifier = Modifier.fillMaxSize(),
                        summary = summary!!,
                        onShareClick = viewModel::shareReferralInvite,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReferralLoadedContent(
    summary: ReferralSummary,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasVerifiedReferralBadge = summary.isVerified ||
        (summary.requiredInvitesForVerified > 0 &&
            summary.joinedCount >= summary.requiredInvitesForVerified)
    val progressTarget = if (summary.requiredInvitesForVerified <= 0) {
        0f
    } else {
        summary.joinedCount.toFloat() / summary.requiredInvitesForVerified.toFloat()
    }.coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing),
        label = "referral_progress",
    )

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            horizontal = DPExtraLarge,
            vertical = DPLarge,
        ),
        verticalArrangement = Arrangement.spacedBy(DPLarge),
    ) {
        item("share") {
            AnimatedSection(delayMillis = 40) {
                ReferralHeroCard(
                    summary = summary,
                    onShareClick = onShareClick,
                )
            }
        }

        item("progress") {
            AnimatedSection(delayMillis = 95) {
                ReferralProgressCard(
                    summary = summary,
                    progress = progress,
                )
            }
        }

        item("badge") {
            AnimatedSection(delayMillis = 150) {
                ReferralBadgeCard(
                    progress = progress,
                    hasVerifiedReferralBadge = hasVerifiedReferralBadge,
                )
            }
        }
    }
}

@Composable
private fun ReferralHeroCard(
    summary: ReferralSummary,
    onShareClick: () -> Unit,
) {
    PostContainer(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                clip = true
                shape = RoundedCornerShape(24.dp)
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.78f),
                        ),
                    ),
                )
                .padding(horizontal = DPExtraLarge, vertical = DPLarge),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(DPMedium)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(DPMedium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.42f),
                    ) {
                        Icon(
                            modifier = Modifier.padding(DPMedium),
                            imageVector = Icons.Rounded.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = stringResource(R.string.referral_share_card_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DPMedium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        ),
                    ) {
                        Text(
                            text = summary.referralCode,
                            modifier = Modifier.padding(
                                horizontal = DPLarge,
                                vertical = DPMedium,
                            ),
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = "${summary.joinedCount}/${summary.requiredInvitesForVerified}",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }

                Text(
                    text = stringResource(
                        R.string.referral_joined_count_format,
                        summary.joinedCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )

                Button(
                    onClick = onShareClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(stringResource(R.string.referral_share_button))
                }
            }
        }
    }
}

@Composable
private fun ReferralProgressCard(
    summary: ReferralSummary,
    progress: Float,
) {
    PostContainer(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                .padding(DPExtraLarge),
            verticalArrangement = Arrangement.spacedBy(DPMedium),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DPSmall),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Group,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(
                        R.string.referral_join_goal_format,
                        summary.requiredInvitesForVerified,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                color = MaterialTheme.colorScheme.primary,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    text = stringResource(
                        R.string.referral_remaining_to_verified_format,
                        summary.remainingToVerified,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReferralBadgeCard(
    progress: Float,
    hasVerifiedReferralBadge: Boolean,
) {
    val transition = rememberInfiniteTransition(label = "referral_badge")
    val badgeScale by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "referral_badge_scale",
    )
    val glowAlpha by transition.animateFloat(
        initialValue = if (hasVerifiedReferralBadge) 0.36f else 0.22f,
        targetValue = if (hasVerifiedReferralBadge) 0.7f else 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "referral_badge_glow",
    )
    val badgeGlowColor = MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha)
    val badgeInnerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f)

    PostContainer(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.88f),
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                        ),
                    ),
                )
                .padding(DPExtraLarge),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(DPMedium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(94.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            color = badgeGlowColor,
                            radius = size.minDimension * (0.49f + progress * 0.04f),
                            center = center,
                        )
                        drawCircle(
                            color = badgeInnerColor,
                            radius = size.minDimension * 0.3f,
                            center = center,
                        )
                    }
                    Surface(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = badgeScale
                                scaleY = badgeScale
                            }
                            .size(66.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        ),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            UserBadgeIcon(badge = "verified", size = 40.dp)
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(DPSmall),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(DPSmall),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.referral_verified_badge_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (hasVerifiedReferralBadge) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    Text(
                        text = stringResource(R.string.referral_verified_badge_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
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
        label = "referral_section_alpha",
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 24f,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "referral_section_offset",
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.96f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
        label = "referral_section_scale",
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            translationY = offsetY
            scaleX = scale
            scaleY = scale
        },
    ) {
        content()
    }
}

@Composable
private fun ReferralAnimatedBackdrop(modifier: Modifier = Modifier) {
    val primaryTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    val secondaryTint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
    val transition = rememberInfiniteTransition(label = "referral_background")
    val firstBlobProgress by transition.animateFloat(
        initialValue = 0.08f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12_500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "referral_blob_1",
    )
    val secondBlobProgress by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = -0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14_500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "referral_blob_2",
    )

    Canvas(modifier = modifier) {
        drawCircle(
            color = primaryTint,
            radius = size.minDimension * 0.46f,
            center = Offset(
                x = size.width * firstBlobProgress,
                y = size.height * 0.15f,
            ),
        )
        drawCircle(
            color = secondaryTint,
            radius = size.minDimension * 0.39f,
            center = Offset(
                x = size.width * secondBlobProgress,
                y = size.height * 0.88f,
            ),
        )
    }
}
