package com.dino.message.threadfeature.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dino.message.corefeature.presentation.viewmodel.rememberStateHolder
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.threadfeature.presentation.component.ThreadCommentsSection
import com.dino.message.threadfeature.presentation.component.ThreadCommentsSectionStyle
import com.dino.message.threadfeature.presentation.viewmodel.ThreadCommentsViewModel

@Composable
fun ThreadCommentsBottomSheetScreen(
    postId: Long
) {
    val viewModel: ThreadCommentsViewModel = rememberStateHolder(postId)

    LaunchedEffect(postId) {
        viewModel.setPostId(postId)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DPMedium, vertical = DPSmall)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.18f)
                    .height(4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape
            ) {}
        }

        ThreadCommentsSection(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = DPMedium),
            viewModel = viewModel,
            style = ThreadCommentsSectionStyle.BottomSheet
        )
    }
}
