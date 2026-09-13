package com.dino.message.profilefeature.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.dino.message.corefeature.presentation.viewmodel.rememberStateHolder
import com.dino.message.profilefeature.presentation.viewmodel.DeletePostBottomSheetViewModel

@Composable
fun DeletePostBottomSheetScreen(
    postId: Long,
) {
    val viewModel: DeletePostBottomSheetViewModel = rememberStateHolder(postId)
    val isDeleting by viewModel.isDeleting.collectAsState()
    val error by viewModel.error.collectAsState()

    DeletePostBottomSheetContent(
        postId = postId,
        isDeleting = isDeleting,
        error = error,
        onClose = viewModel::closeSheet,
        onConfirmDelete = { viewModel.confirmDelete(postId) },
    )
}
