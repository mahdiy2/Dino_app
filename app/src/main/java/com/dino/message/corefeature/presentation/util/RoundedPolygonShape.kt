package com.dino.message.corefeature.presentation.util

import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath

class RoundedPolygonShape(private val roundedPolygon: RoundedPolygon) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val androidPath = roundedPolygon.toPath()
        val composePath = androidPath.asComposePath()

        val matrix = Matrix()
        matrix.translate(1f, 1f)
        matrix.scale(size.width / 2f, size.height / 2f)

        composePath.transform(matrix)

        return Outline.Generic(composePath)
    }
}
