package com.dino.message.corefeature.presentation.util

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.dino.message.corefeature.presentation.platform.isPlatformImageLoading
import com.dino.message.corefeature.presentation.platform.rememberPlatformImagePainter


@Composable
fun LoadImageWithCoil(imageUrl: String, modifier: Modifier) {
    val painter = rememberPlatformImagePainter(model = imageUrl)

    Box(modifier = modifier) {
        if (painter.isPlatformImageLoading) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(bottom = 24.dp, end = 24.dp)
                        .size(24.dp),
                    color = Color.White
                )
            }
        }

        Image(
            painter = painter,
            contentDescription = "Loaded Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}
