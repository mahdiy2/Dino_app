package com.dino.message.threadfeature.presentation.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Create
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.dino.message.R
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.threadfeature.presentation.viewmodel.CreatePostOptionsBottomSheetScreenViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.bottomsheet.spec.DestinationStyleBottomSheet
import kotlin.math.cos
import kotlin.math.sin


@Destination<RootGraph>(style = DestinationStyleBottomSheet::class)
@Composable
fun CreatePostOptionsBottomSheetScreen(
    quotedPostId: Long = 0L,
){

    val viewModel: CreatePostOptionsBottomSheetScreenViewModel = hiltViewModel()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(bottom = DPExtraLarge)
            .padding(horizontal = DPMedium)
    ) {

//        Spacer(Modifier.height(12.dp))

        // animation
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            PremiumCreatePostAnimation()
        }

//        Spacer(Modifier.height(12.dp))

        Text(
            text = "Choose post type",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = if (viewModel.isQuoteMode) {
                "Quote this post with a normal post or a music post."
            } else {
                "Create a normal post or share one music track."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            Modifier
                .padding(vertical = 16.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    )
                )
        )

        PostOptionItem(
            title = "Create Dino",
            description = "Post text, images, or videos",
            icon = Icons.Rounded.Create,
            onClick = viewModel::openCreatePostScreen
        )

        Spacer(Modifier.height(8.dp))

        PostOptionItem(
            title = "Post Music",
            description = "Share one track with optional text",
            icon = Icons.Rounded.LibraryMusic,
            onClick = viewModel::openPostMusicScreen
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PostOptionItem(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
)
{

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
        tonalElevation = 2.dp
    ) {

        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PremiumCreatePostAnimation() {

    val infinite = rememberInfiniteTransition(label = "")

    // pulse breathing
    val pulse by infinite.animateFloat(
        0.96f,
        1.05f,
        infiniteRepeatable(
            tween(2600, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = ""
    )

    // orbit (organic easing)
    val orbit by infinite.animateFloat(
        0f,
        360f,
        infiniteRepeatable(
            tween(24000, easing = FastOutSlowInEasing)
        ),
        label = ""
    )

    // plus breathing
    val plusPulse by infinite.animateFloat(
        0.95f,
        1.05f,
        infiniteRepeatable(
            tween(2200),
            RepeatMode.Reverse
        ),
        label = ""
    )

    // entrance animation
    var started by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (started) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = 120f
        ),
        label = ""
    )

    LaunchedEffect(Unit) {
        started = true
    }

    // pre‑remember the gradient to avoid re‑creation every frame
    val gradient = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF3A86FF),
                Color(0xFF00D4FF)
            ),
            radius = 140f
        )
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {

        val cx = size.width / 2
        val cy = size.height / 2

        val coreRadius = 50f * pulse * scale

        // soft glow
        drawCircle(
            color = Color(0xFF00D4FF).copy(alpha = 0.12f),
            radius = 130f * pulse,
            center = Offset(cx, cy)
        )

        // orbit ring with gradient atmosphere
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    Color.White.copy(alpha = 0.10f),
                    Color.Transparent
                ),
                radius = 160f,
                center = Offset(cx, cy)
            ),
            radius = 135f,
            center = Offset(cx, cy),
            style = Stroke(width = 2f)
        )

        // orbiting particles
        repeat(2) { i ->
            val angle = Math.toRadians((orbit + i * 180).toDouble())
            val r = 135f

            val x = cx + cos(angle).toFloat() * r
            val y = cy + sin(angle).toFloat() * r

            drawCircle(
                color = Color(0xFF00D4FF),
                radius = 6f,
                center = Offset(x, y),
                alpha = 0.9f
            )
        }

        // core
        drawCircle(
            brush = gradient,
            radius = coreRadius,
            center = Offset(cx, cy)
        )

        // inner shadow (depth)
        drawCircle(
            color = Color.Black.copy(alpha = 0.08f),
            radius = coreRadius,
            center = Offset(cx, cy),
            style = Stroke(width = 4f)
        )

        // highlight
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.30f),
                    Color.Transparent
                ),
                center = Offset(cx - 18, cy - 18),
                radius = coreRadius
            ),
            radius = coreRadius,
            center = Offset(cx, cy)
        )

        // inner glowing ring (premium touch)
        drawCircle(
            color = Color.White.copy(alpha = 0.08f),
            radius = coreRadius * 0.85f,
            center = Offset(cx, cy),
            style = Stroke(width = 2f)
        )

        // plus icon (with breathing)
        val plus = 18f * scale * plusPulse

        drawLine(
            Color.White,
            Offset(cx - plus, cy),
            Offset(cx + plus, cy),
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )

        drawLine(
            Color.White,
            Offset(cx, cy - plus),
            Offset(cx, cy + plus),
            strokeWidth = 8f,
            cap = StrokeCap.Round
        )
    }
}



