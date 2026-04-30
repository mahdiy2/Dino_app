package com.dino.message.groupfeature.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dino.message.groupfeature.domain.model.GroupOnlineUser
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

private data class BubblePlacement(
    val key: String,
    val user: GroupOnlineUser,
    val sizePx: Float,
    val xPx: Float,
    val yPx: Float,
)

@Composable
fun OnlineAvatarBubbleCluster(
    onlineUsers: List<GroupOnlineUser>,
    typingUsers: List<GroupOnlineUser> = emptyList(),
    modifier: Modifier = Modifier,
    maxVisible: Int = 12,
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.roundToPx() }
        val maxHeightPx = with(density) { maxHeight.roundToPx() }
        val minAvatarSizePx = with(density) { 28.dp.toPx() }
        val maxAvatarSizePx = with(density) { 44.dp.toPx() }
        val visibleLimit = remember(maxVisible, maxWidthPx, maxHeightPx) {
            resolveVisibleCountForSpace(
                maxVisible = maxVisible,
                maxWidthPx = maxWidthPx,
                maxHeightPx = maxHeightPx,
                minAvatarSizePx = minAvatarSizePx,
                maxAvatarSizePx = maxAvatarSizePx,
            )
        }

        val onlineUnique = remember(onlineUsers) {
            onlineUsers
                .filter { it.username.isNotBlank() }
                .distinctBy { userKey(it) }
        }
        val availableByKey = remember(onlineUnique) {
            onlineUnique.associateBy { userKey(it) }
        }
        val typingKeys = remember(typingUsers) {
            typingUsers
                .filter { it.username.isNotBlank() }
                .map { userKey(it) }
                .toSet()
        }

        val selectedKeys = remember { mutableStateListOf<String>() }
        val selectionStateKey = remember(onlineUnique, typingKeys, visibleLimit) {
            buildString {
                append(visibleLimit)
                append('#')
                append(onlineUnique.map { userKey(it) }.sorted().joinToString("|"))
                append('#')
                append(typingKeys.sorted().joinToString("|"))
            }
        }
        LaunchedEffect(selectionStateKey) {
            if (visibleLimit <= 0) {
                selectedKeys.clear()
                return@LaunchedEffect
            }
            val random = Random(selectionStateKey.hashCode() xor selectedKeys.hashCode())
            selectedKeys.removeAll { key -> key !in availableByKey }

            // Typing users are always promoted into the visible list.
            typingKeys.forEach { typingKey ->
                if (typingKey in selectedKeys) return@forEach
                if (selectedKeys.size < visibleLimit) {
                    selectedKeys += typingKey
                } else {
                    val replaceable = selectedKeys.filter { it !in typingKeys }
                    if (replaceable.isNotEmpty()) {
                        val victim = replaceable[random.nextInt(replaceable.size)]
                        selectedKeys[selectedKeys.indexOf(victim)] = typingKey
                    }
                }
            }

            if (selectedKeys.size < visibleLimit) {
                val missing = visibleLimit - selectedKeys.size
                val candidates = availableByKey.keys
                    .filter { it !in selectedKeys }
                    .shuffled(random)
                    .take(missing)
                selectedKeys.addAll(candidates)
            }

            while (selectedKeys.size > visibleLimit) {
                val removable = selectedKeys.filter { it !in typingKeys }
                if (removable.isNotEmpty()) {
                    val victim = removable[random.nextInt(removable.size)]
                    selectedKeys.remove(victim)
                } else {
                    selectedKeys.removeLast()
                }
            }
        }
        val selectedUsers = selectedKeys.mapNotNull { availableByKey[it] }

        val layoutKey = remember(selectedUsers, maxWidthPx, maxHeightPx) {
            selectedUsers.joinToString(separator = "|") { userKey(it) } + "#$maxWidthPx#$maxHeightPx"
        }
        val targetPlacements = remember(layoutKey, minAvatarSizePx, maxAvatarSizePx) {
            buildBubblePlacements(
                users = selectedUsers,
                maxWidthPx = maxWidthPx,
                maxHeightPx = maxHeightPx,
                minAvatarSizePx = minAvatarSizePx,
                maxAvatarSizePx = maxAvatarSizePx,
                seed = layoutKey.hashCode(),
            )
        }
        val hiddenOnlineCount = (onlineUnique.size - selectedUsers.size).coerceAtLeast(0)
        val placementByKey = remember(targetPlacements) {
            targetPlacements.associateBy { it.key }
        }
        val globalMotionTransition = rememberInfiniteTransition(label = "cluster_motion")
        val globalMotionState = globalMotionTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 7500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "cluster_motion_phase",
        )
        val typingOrbitTransition = rememberInfiniteTransition(label = "typing_orbit")
        val typingOrbitState = typingOrbitTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1100, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "typing_orbit_base",
        )
        val typingPulseTransition = rememberInfiniteTransition(label = "typing_pulse")
        val typingPulseState = typingPulseTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 900, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "typing_pulse_phase",
        )

        val renderedPlacements = remember { mutableStateMapOf<String, BubblePlacement>() }
        val exiting = remember { mutableStateMapOf<String, Boolean>() }

        LaunchedEffect(targetPlacements) {
            val currentKeys = targetPlacements.map { it.key }.toSet()
            targetPlacements.forEach { renderedPlacements[it.key] = it }
            exiting.keys.toList().forEach { if (it in currentKeys) exiting.remove(it) }
            val removed = renderedPlacements.keys.filter { it !in currentKeys }
            removed.forEach { exiting[it] = true }
            if (removed.isNotEmpty()) {
                delay(240)
                removed.forEach { key ->
                    if (exiting[key] == true) {
                        exiting.remove(key)
                        renderedPlacements.remove(key)
                    }
                }
            }
        }

        renderedPlacements.values.sortedBy { it.yPx }.forEach { entry ->
            key(entry.key) {
                val target = placementByKey[entry.key] ?: entry
                val animatedX by animateFloatAsState(
                    targetValue = target.xPx,
                    animationSpec = tween(500, easing = FastOutSlowInEasing),
                    label = "bubble_x_${entry.key}",
                )
                val animatedY by animateFloatAsState(
                    targetValue = target.yPx,
                    animationSpec = tween(500, easing = FastOutSlowInEasing),
                    label = "bubble_y_${entry.key}",
                )
                val animatedSize by animateFloatAsState(
                    targetValue = target.sizePx,
                    animationSpec = tween(500, easing = FastOutSlowInEasing),
                    label = "bubble_size_${entry.key}",
                )
                val isTyping = entry.key in typingKeys
                val typingAlpha by animateFloatAsState(
                    targetValue = if (isTyping) 1f else 0f,
                    animationSpec = tween(320, easing = FastOutSlowInEasing),
                    label = "typing_alpha_${entry.key}",
                )
                val typingProgress by animateFloatAsState(
                    targetValue = if (isTyping) 1f else 0f,
                    animationSpec = tween(420, easing = FastOutSlowInEasing),
                    label = "typing_progress_${entry.key}",
                )

                val seed = remember(entry.key) { absSeed(entry.key) }
                val horizontalAmplitude = with(density) { (2.0f + (seed % 10) / 10f).dp.toPx() }
                val verticalAmplitude = with(density) { (1.6f + (seed % 8) / 10f).dp.toPx() }
                val phaseX = remember(entry.key) { ((seed % 360) / 180f * PI).toFloat() }
                val phaseY = remember(entry.key) { (((seed / 2) % 360) / 180f * PI).toFloat() }
                val phaseS = remember(entry.key) { (((seed / 3) % 360) / 180f * PI).toFloat() }

                AnimatedVisibility(
                    visible = exiting[entry.key] != true,
                    enter = fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.8f),
                    exit = fadeOut(tween(220)) + scaleOut(tween(220), targetScale = 0.86f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(with(density) { animatedSize.toDp() })
                            .padding(1.dp)
                            .graphicsLayer {
                                val phase = globalMotionState.value
                                val driftX = sin((phase + phaseX).toDouble()).toFloat()
                                val driftY = cos((phase + phaseY).toDouble()).toFloat()
                                val baseScale = sin((phase + phaseS).toDouble()).toFloat() * 0.03f
                                val typingScalePulse =
                                    0.055f + (sin((typingPulseState.value + phaseS).toDouble()).toFloat() * 0.06f)
                                val typingScale = typingProgress * typingScalePulse
                                val centerX = ((maxWidthPx - animatedSize) / 2f)
                                val centerY = ((maxHeightPx - animatedSize) / 2f)
                                val towardCenterFactor = when {
                                    selectedUsers.size <= 2 -> 0.34f
                                    selectedUsers.size == 3 -> 0.20f
                                    else -> 0f
                                }
                                val typingMotionBoost = 1f + (typingProgress * 0.75f)
                                val pullX = (centerX - animatedX) * towardCenterFactor
                                val pullY = (centerY - animatedY) * towardCenterFactor
                                val scale = 1f + baseScale + typingScale
                                translationX = animatedX + pullX + (driftX * horizontalAmplitude * typingMotionBoost)
                                translationY = animatedY + pullY + (driftY * verticalAmplitude * typingMotionBoost)
                                scaleX = scale.coerceIn(0.92f, 1.08f)
                                scaleY = scale.coerceIn(0.92f, 1.08f)
                                alpha = 1f
                            }
                    ) {
                        AvatarBubble(
                            user = target.user,
                            typingAlpha = typingAlpha,
                            orbitAngleState = typingOrbitState,
                            orbitSeed = seed % 90,
                        )
                    }
                }
            }
        }

        if (hiddenOnlineCount > 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 4.dp, bottom = 2.dp),
                shape = RoundedCornerShape(999.dp),
                tonalElevation = 3.dp,
                shadowElevation = 1.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Text(
                    text = "+$hiddenOnlineCount online",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun AvatarBubble(
    user: GroupOnlineUser,
    typingAlpha: Float,
    orbitAngleState: State<Float>,
    orbitSeed: Int,
) {
    val avatarShape = CircleShape

    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = avatarShape,
        tonalElevation = 3.dp,
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        val imageUrl = user.profileImageUrl?.takeIf { it.isNotBlank() }
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = user.username,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = user.username.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        if (typingAlpha > 0f) {
            val primary = MaterialTheme.colorScheme.primary
            Canvas(modifier = Modifier.fillMaxSize()) {
                val ringStroke = 2.dp.toPx()
                val dotRadius = 3.2.dp.toPx()
                val center = Offset(size.width / 2f, size.height / 2f)
                val ringRadius = ((size.minDimension - ringStroke) / 2f).coerceAtLeast(0f)
                val orbitRadius = (ringRadius - dotRadius).coerceAtLeast(0f)
                val orbitAngleDegrees = ((orbitAngleState.value + orbitSeed.toFloat()) % 360f + 360f) % 360f
                val angleRad = ((orbitAngleDegrees / 180f) * PI).toFloat()
                val dotCenter = Offset(
                    x = center.x + (cos(angleRad.toDouble()).toFloat() * orbitRadius),
                    y = center.y + (sin(angleRad.toDouble()).toFloat() * orbitRadius),
                )

                drawCircle(
                    color = primary.copy(alpha = 0.95f * typingAlpha),
                    radius = ringRadius,
                    style = Stroke(width = ringStroke, cap = StrokeCap.Round),
                )
                drawCircle(
                    color = primary.copy(alpha = 0.25f * typingAlpha),
                    radius = dotRadius * 1.7f,
                    center = dotCenter,
                )
                drawCircle(
                    color = primary.copy(alpha = typingAlpha),
                    radius = dotRadius,
                    center = dotCenter,
                )
            }
        }
    }
}

private fun buildBubblePlacements(
    users: List<GroupOnlineUser>,
    maxWidthPx: Int,
    maxHeightPx: Int,
    minAvatarSizePx: Float,
    maxAvatarSizePx: Float,
    seed: Int,
): List<BubblePlacement> {
    if (users.isEmpty() || maxWidthPx <= 0 || maxHeightPx <= 0) return emptyList()

    val random = Random(seed)
    val minSizePx = minAvatarSizePx.roundToInt().coerceAtLeast(18)
    val maxSizePx = maxAvatarSizePx.roundToInt().coerceAtLeast(minSizePx)
    val placements = mutableListOf<BubblePlacement>()

    users.forEach { user ->
        val sizePx = random.nextInt(minSizePx, maxSizePx + 1).toFloat()
            .coerceAtMost(min(maxWidthPx, maxHeightPx).toFloat())
        val maxX = max(0f, maxWidthPx - sizePx)
        val maxY = max(0f, maxHeightPx - sizePx)
        val spread = when {
            users.size <= 1 -> 0f
            users.size == 2 -> 0.22f
            users.size == 3 -> 0.36f
            users.size <= 6 -> 0.72f
            else -> 1f
        }
        val minX = ((maxX / 2f) - ((maxX * spread) / 2f)).coerceAtLeast(0f)
        val maxSpreadX = ((maxX / 2f) + ((maxX * spread) / 2f)).coerceAtMost(maxX)
        val minY = ((maxY / 2f) - ((maxY * spread) / 2f)).coerceAtLeast(0f)
        val maxSpreadY = ((maxY / 2f) + ((maxY * spread) / 2f)).coerceAtMost(maxY)
        val separationPaddingPx = 2.5f

        var firstNonOverlapping: BubblePlacement? = null
        var best: BubblePlacement? = null
        var bestScore = Float.MAX_VALUE
        repeat(48) {
            val candidate = BubblePlacement(
                key = userKey(user),
                user = user,
                sizePx = sizePx,
                xPx = randomInRange(minX, maxSpreadX, random),
                yPx = randomInRange(minY, maxSpreadY, random),
            )
            if (!intersectsExisting(candidate, placements, separationPaddingPx)) {
                firstNonOverlapping = candidate
                return@repeat
            }
            val score = overlapScore(candidate, placements)
            if (score < bestScore) {
                bestScore = score
                best = candidate
            }
        }
        placements += firstNonOverlapping ?: best ?: BubblePlacement(
            key = userKey(user),
            user = user,
            sizePx = sizePx,
            xPx = maxX / 2f,
            yPx = maxY / 2f,
        )
    }
    return placements
}

private fun overlapScore(
    candidate: BubblePlacement,
    existing: List<BubblePlacement>,
): Float {
    if (existing.isEmpty()) return 0f
    var score = 0f
    existing.forEach { other ->
        val overlapWidth = min(candidate.xPx + candidate.sizePx, other.xPx + other.sizePx) - max(candidate.xPx, other.xPx)
        val overlapHeight = min(candidate.yPx + candidate.sizePx, other.yPx + other.sizePx) - max(candidate.yPx, other.yPx)
        if (overlapWidth <= 0f || overlapHeight <= 0f) return@forEach
        val overlapArea = overlapWidth * overlapHeight
        val smallerArea = min(candidate.sizePx * candidate.sizePx, other.sizePx * other.sizePx)
        val overlapRatio = overlapArea / smallerArea
        score += overlapRatio
        if (overlapRatio > 0.72f) score += 4f
    }
    return score
}

private fun intersectsExisting(
    candidate: BubblePlacement,
    existing: List<BubblePlacement>,
    paddingPx: Float,
): Boolean {
    return existing.any { other ->
        val left = candidate.xPx
        val right = candidate.xPx + candidate.sizePx
        val top = candidate.yPx
        val bottom = candidate.yPx + candidate.sizePx

        val otherLeft = other.xPx - paddingPx
        val otherRight = other.xPx + other.sizePx + paddingPx
        val otherTop = other.yPx - paddingPx
        val otherBottom = other.yPx + other.sizePx + paddingPx

        right > otherLeft && left < otherRight && bottom > otherTop && top < otherBottom
    }
}

private fun resolveVisibleCountForSpace(
    maxVisible: Int,
    maxWidthPx: Int,
    maxHeightPx: Int,
    minAvatarSizePx: Float,
    maxAvatarSizePx: Float,
): Int {
    if (maxVisible <= 1) return 1
    if (maxWidthPx <= 0 || maxHeightPx <= 0) return maxVisible
    val averageDiameter = (minAvatarSizePx + maxAvatarSizePx) / 2f
    val averageArea = averageDiameter * averageDiameter
    val containerArea = maxWidthPx.toFloat() * maxHeightPx.toFloat()
    val estimated = ((containerArea * 0.88f) / averageArea * 1.25f).toInt().coerceAtLeast(2)
    return min(maxVisible, estimated)
}

private fun randomInRange(minValue: Float, maxValue: Float, random: Random): Float {
    if (maxValue <= minValue) return minValue
    return minValue + (random.nextFloat() * (maxValue - minValue))
}

private fun userKey(user: GroupOnlineUser): String = user.userId.ifBlank { user.username }

private fun absSeed(key: String): Int = key.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }
