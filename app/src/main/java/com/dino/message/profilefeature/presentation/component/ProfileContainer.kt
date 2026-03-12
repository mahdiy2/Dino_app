package com.dino.message.profilefeature.presentation.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge

@Composable
fun ProfileContainer(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(bottomEnd = 28.dp, bottomStart = 28.dp),
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
    ProfileContainer(
    ){
        Text("Hello World")
    }
}