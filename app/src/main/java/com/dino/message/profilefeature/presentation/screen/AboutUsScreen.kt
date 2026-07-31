package com.dino.message.profilefeature.presentation.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import coil.compose.SubcomposeAsyncImage
import com.dino.message.R
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.corefeature.presentation.util.backIconModifier
import com.dino.message.profilefeature.domain.model.AboutUsLink
import com.dino.message.profilefeature.domain.model.AboutUsTeamMember
import com.dino.message.profilefeature.presentation.viewmodel.AboutUsViewModel
import com.dino.message.threadfeature.presentation.component.PostContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutUsScreen() {
    val viewModel: AboutUsViewModel = koinViewModel()
    val aboutUs by viewModel.aboutUs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.about_us)) },
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
            AboutUsAnimatedBackdrop(
                modifier = Modifier.fillMaxSize(),
            )

            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = DPExtraLarge,
                        vertical = DPLarge,
                    ),
                    verticalArrangement = Arrangement.spacedBy(DPLarge),
                ) {
                    if (error != null) {
                        item("error") {
                            AnimatedSection(delayMillis = 40) {
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

                    if (isLoading && aboutUs == null) {
                        item("loading") {
                            AnimatedSection(delayMillis = 80) {
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
                    }

                    val currentData = aboutUs

                    item("hero") {
                        AnimatedSection(delayMillis = 90) {
                            AboutUsHeroCard(
                                description = currentData?.description
                                    ?.takeIf { it.isNotBlank() }
                                    ?: stringResource(R.string.about_us_description),
                                linksCount = currentData?.links?.count { !it.isDisabled } ?: 0,
                                teamCount = currentData?.teamMembers?.size ?: 0,
                            )
                        }
                    }

                    if (currentData != null) {
                        item("links_header") {
                            AnimatedSection(delayMillis = 130) {
                                SectionHeader(
                                    title = stringResource(R.string.about_us_links),
                                    icon = Icons.Rounded.Link,
                                )
                            }
                        }

                        if (currentData.links.isEmpty()) {
                            item("links_empty") {
                                AnimatedSection(delayMillis = 160) {
                                    Text(
                                        text = stringResource(R.string.about_us_links_empty),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(
                                items = currentData.links,
                                key = { _, link -> "${link.type}:${link.title}" },
                            ) { index, link ->
                                AnimatedSection(
                                    delayMillis = 180 + minOf(index * 45, 240),
                                ) {
                                    AboutLinkCard(
                                        link = link,
                                        onClick = {
                                            val opened = openAboutUsLink(context, link)
                                            if (!opened) {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        if (link.type.equals("email", ignoreCase = true)) {
                                                            context.getString(R.string.about_us_email_app_failed)
                                                        } else {
                                                            context.getString(R.string.about_us_open_link_failed)
                                                        }
                                                    )
                                                }
                                            }
                                        },
                                    )
                                }
                            }
                        }

                        item("team_header") {
                            AnimatedSection(delayMillis = 220) {
                                SectionHeader(
                                    title = stringResource(R.string.about_us_team_members),
                                    icon = Icons.Rounded.Person,
                                )
                            }
                        }

                        if (currentData.teamMembers.isEmpty()) {
                            item("team_empty") {
                                AnimatedSection(delayMillis = 250) {
                                    Text(
                                        text = stringResource(R.string.about_us_team_empty),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            val teamRows = currentData.teamMembers.chunked(2)
                            itemsIndexed(
                                items = teamRows,
                                key = { _, row -> row.first().userId },
                            ) { index, rowMembers ->
                                AnimatedSection(delayMillis = 250 + minOf(index * 45, 250)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(DPMedium),
                                    ) {
                                        rowMembers.forEach { member ->
                                            TeamMemberColumnItem(
                                                modifier = Modifier.weight(1f),
                                                member = member,
                                                onClick = { viewModel.openUserProfile(member.username) },
                                            )
                                        }
                                        if (rowMembers.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!isLoading && currentData == null) {
                        item("empty") {
                            AnimatedSection(delayMillis = 140) {
                                Text(
                                    text = stringResource(R.string.about_us_empty),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutUsAnimatedBackdrop(modifier: Modifier = Modifier) {
    val primaryTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
    val tertiaryTint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
    val transition = rememberInfiniteTransition(label = "about_us_background")
    val firstBlobProgress by transition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "about_us_blob_1",
    )
    val secondBlobProgress by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = -0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 13_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "about_us_blob_2",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "about_us_blob_pulse",
    )

    Canvas(modifier = modifier) {
        drawCircle(
            color = primaryTint,
            radius = size.minDimension * 0.42f * pulse,
            center = Offset(
                x = size.width * firstBlobProgress,
                y = size.height * 0.15f,
            ),
        )
        drawCircle(
            color = tertiaryTint,
            radius = size.minDimension * 0.35f * pulse,
            center = Offset(
                x = size.width * secondBlobProgress,
                y = size.height * 0.88f,
            ),
        )
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

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 450),
        ) + slideInVertically(
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            initialOffsetY = { it / 5 },
        ),
        exit = fadeOut(animationSpec = tween(durationMillis = 150)),
    ) {
        content()
    }
}

@Composable
private fun AboutUsHeroCard(
    description: String,
    linksCount: Int,
    teamCount: Int,
) {
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
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.86f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f),
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
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = stringResource(R.string.about_us),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DPMedium),
                ) {
                    HeroStatChip(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Link,
                        label = stringResource(R.string.about_us_links),
                        value = linksCount,
                    )
                    HeroStatChip(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Person,
                        label = stringResource(R.string.about_us_team_members),
                        value = teamCount,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroStatChip(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: Int,
) {
    Surface(
        modifier = modifier,
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
                imageVector = icon,
                contentDescription = null,
            )
            Column {
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    icon: ImageVector,
) {
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
                imageVector = icon,
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
private fun AboutLinkCard(
    link: AboutUsLink,
    onClick: () -> Unit,
) {
    val enabled = !link.isDisabled
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.98f else 1f,
        animationSpec = spring(),
        label = "about_link_scale",
    )

    PostContainer(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(MaterialTheme.shapes.extraLarge)
            .alpha(if (enabled) 1f else 0.62f)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (enabled) 8.dp else 2.dp,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DPLarge, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(DPMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Icon(
                    modifier = Modifier.padding(10.dp),
                    imageVector = iconForLinkType(link.type),
                    contentDescription = null,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(DPSmall),
            ) {
                Text(
                    text = link.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = linkSubtitle(link),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (link.isDisabled) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = {
                        Text(stringResource(R.string.about_us_link_disabled))
                    },
                )
            }
        }
    }
}

@Composable
private fun TeamMemberColumnItem(
    modifier: Modifier = Modifier,
    member: AboutUsTeamMember,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(),
        label = "team_member_scale",
    )

    PostContainer(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(MaterialTheme.shapes.large)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (pressed) 6.dp else 10.dp,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = DPMedium, vertical = DPLarge),
            verticalArrangement = Arrangement.spacedBy(DPSmall),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.size(58.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                if (member.profileImageUrl.isNullOrBlank()) {
                    Icon(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(DPMedium),
                        imageVector = Icons.Rounded.Person,
                        contentDescription = stringResource(R.string.about_us_member_avatar),
                    )
                } else {
                    SubcomposeAsyncImage(
                        model = member.profileImageUrl,
                        contentDescription = stringResource(R.string.about_us_member_avatar),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Icon(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(DPMedium),
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                            )
                        },
                        error = {
                            Icon(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(DPMedium),
                                imageVector = Icons.Rounded.Person,
                                contentDescription = null,
                            )
                        },
                    )
                }
            }

            Text(
                text = member.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = member.role,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun linkSubtitle(link: AboutUsLink): String {
    val raw = link.url.trim()
    if (raw.isBlank()) return link.type

    return if (link.type.equals("email", ignoreCase = true)) {
        extractEmailAddress(raw)
    } else {
        raw.removePrefix("https://").removePrefix("http://")
    }
}

private fun iconForLinkType(type: String) = when (type.lowercase()) {
    "website" -> Icons.Rounded.Language
    "source_code" -> Icons.Rounded.Settings
    "email" -> Icons.Rounded.Forum
    else -> Icons.Rounded.Link
}

private fun openAboutUsLink(context: Context, link: AboutUsLink): Boolean {
    if (link.url.isBlank()) return false
    return if (link.type.equals("email", ignoreCase = true)) {
        openEmailApp(context, link.url)
    } else {
        openInBrowser(context, link.url)
    }
}

private fun openEmailApp(context: Context, rawValue: String): Boolean {
    val trimmed = rawValue.trim()
    val emailAddress = extractEmailAddress(trimmed)
    if (emailAddress.isBlank()) return false

    val mailtoUri = if (trimmed.startsWith("mailto:", ignoreCase = true)) {
        Uri.parse(trimmed)
    } else {
        Uri.fromParts("mailto", emailAddress, null)
    }
    return try {
        val intent = Intent(Intent.ACTION_SENDTO, mailtoUri).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }
}

private fun extractEmailAddress(value: String): String {
    if (value.startsWith("mailto:", ignoreCase = true)) {
        val schemeSpecificPart = Uri.parse(value).schemeSpecificPart.orEmpty()
        return schemeSpecificPart.substringBefore("?").trim()
    }
    return value.substringBefore("?").trim()
}

private fun openInBrowser(context: Context, url: String): Boolean {
    return try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }
}
