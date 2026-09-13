package com.dino.message.threadfeature.presentation.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun PostContainer(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
    borderColor: Color? = null,
    content: @Composable (ColumnScope.() -> Unit)
) {
    Card(
        modifier = modifier,
        shape = shape,
        elevation = elevation,
        border = if (borderColor != null) {
            androidx.compose.foundation.BorderStroke(1.5.dp, borderColor)
        } else {
            null
        }
    ) {
        content()
    }
}
