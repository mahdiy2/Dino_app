package com.dino.message.corefeature.presentation.util

import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.Size
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class RoundedPolygonShape(
    private val vertices: Int,
    private val cornerSmoothing: Float = 0.18f,
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val safeVertices = vertices.coerceAtLeast(3)
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = minOf(size.width, size.height) / 2f
        val points = List(safeVertices) { index ->
            val angle = -PI / 2.0 + (2.0 * PI * index / safeVertices)
            androidx.compose.ui.geometry.Offset(
                x = centerX + cos(angle).toFloat() * radius,
                y = centerY + sin(angle).toFloat() * radius,
            )
        }
        val smoothing = cornerSmoothing.coerceIn(0f, 0.45f)
        val path = Path()

        points.forEachIndexed { index, point ->
            val previous = points[(index - 1 + safeVertices) % safeVertices]
            val next = points[(index + 1) % safeVertices]
            val start = point.lerp(previous, smoothing)
            val end = point.lerp(next, smoothing)

            if (index == 0) {
                path.moveTo(start.x, start.y)
            } else {
                path.lineTo(start.x, start.y)
            }
            path.quadraticTo(point.x, point.y, end.x, end.y)
        }
        path.close()

        return Outline.Generic(path)
    }
}

private fun androidx.compose.ui.geometry.Offset.lerp(
    other: androidx.compose.ui.geometry.Offset,
    fraction: Float,
): androidx.compose.ui.geometry.Offset {
    return androidx.compose.ui.geometry.Offset(
        x = x + (other.x - x) * fraction,
        y = y + (other.y - y) * fraction,
    )
}
