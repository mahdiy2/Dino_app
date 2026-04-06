package com.dino.message.reportfeature.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.reportfeature.presentation.viewmodel.ReportPostBottomSheetViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.bottomsheet.spec.DestinationStyleBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = DestinationStyleBottomSheet::class)
@Composable
fun ReportPostBottomSheetScreen(
    postId: Long,
) {
    val viewModel: ReportPostBottomSheetViewModel = hiltViewModel()
    val isTypesLoading by viewModel.isTypesLoading.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedType = viewModel.selectedType.value
    val reportTypes = viewModel.reportTypes
    val description = viewModel.description.value

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(DPMedium)
            .windowInsetsPadding(WindowInsets.navigationBars),
        verticalArrangement = Arrangement.spacedBy(DPMedium)
    ) {
        Text(
            text = "Report Post",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Select a reason for reporting this post.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Post #$postId",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isTypesLoading && reportTypes.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (reportTypes.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(DPMedium),
                    verticalArrangement = Arrangement.spacedBy(DPSmall)
                ) {
                    Text(
                        text = "Could not load report types.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    FilledTonalButton(
                        onClick = viewModel::loadReportTypes,
                        enabled = !isTypesLoading && !isSubmitting
                    ) {
                        Text("Retry")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                verticalArrangement = Arrangement.spacedBy(DPSmall)
            ) {
                items(reportTypes, key = { it }) { type ->
                    FilterChip(
                        modifier = Modifier.fillMaxWidth(),
                        selected = selectedType == type,
                        onClick = { viewModel.onSelectType(type) },
                        label = {
                            Text(
                                reportTypeLabel(type),
                                modifier = Modifier
                                    .fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors()
                    )
                }
            }
        }

        if (selectedType != null) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = description,
                onValueChange = viewModel::onDescriptionChange,
                enabled = !isSubmitting,
                placeholder = {
                    Text("Optional description")
                },
                minLines = 3,
                maxLines = 5,
                shape = MaterialTheme.shapes.extraLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "${description.length}/255",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (error != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    modifier = Modifier.padding(DPMedium),
                    text = error.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            FilledTonalButton(
                onClick = viewModel::submitReport,
                enabled = !isSubmitting && selectedType != null,
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .width(18.dp)
                            .height(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submitting...")
                } else {
                    Text("Confirm Report")
                }
            }
        }
    }
}

private fun reportTypeLabel(type: String): String {
    return when (type) {
        "scam" -> "Scam"
        "unethical_content" -> "Unethical Content"
        "violence" -> "Violence"
        "spam" -> "Spam"
        "false_information" -> "False Information"
        else -> type.replace("_", " ")
    }
}
