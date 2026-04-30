package com.dino.message.threadfeature.presentation.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dino.message.R

@Composable
fun PostContainer(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    elevation: CardElevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
    content: @Composable (ColumnScope.() -> Unit)
) {
    Card(
        modifier = modifier,
        shape = shape,
        elevation = elevation
    ) {
        content()
    }
}

@Preview(showSystemUi = true)
@Composable
private fun Preview(){
    PostContainer(
    ){
        Text(stringResource(R.string.hellooo))
    }
}
