package com.dino.message.reportfeature.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.dino.message.corefeature.presentation.viewmodel.rememberStateHolder
import com.dino.message.reportfeature.presentation.viewmodel.ReportPostBottomSheetViewModel

@Composable
fun ReportPostBottomSheetScreen(
    postId: Long,
) {
    val viewModel: ReportPostBottomSheetViewModel = rememberStateHolder(postId)
    val isTypesLoading by viewModel.isTypesLoading.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val error by viewModel.error.collectAsState()

    ReportPostBottomSheetContent(
        postId = postId,
        isTypesLoading = isTypesLoading,
        isSubmitting = isSubmitting,
        error = error,
        selectedType = viewModel.selectedType.value,
        reportTypes = viewModel.reportTypes,
        description = viewModel.description.value,
        onLoadReportTypes = viewModel::loadReportTypes,
        onSelectType = viewModel::onSelectType,
        onDescriptionChange = viewModel::onDescriptionChange,
        onSubmitReport = { viewModel.submitReport(postId) },
    )
}
