package com.dino.message.reportfeature.presentation.screen

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.dino.message.R
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.ui.theme.DPSmall
import com.dino.message.reportfeature.presentation.viewmodel.ReportPostBottomSheetViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportPostBottomSheetScreen(
    postId: Long,
) {
    val viewModel: ReportPostBottomSheetViewModel = koinViewModel()
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
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(DPMedium),
                verticalArrangement = Arrangement.spacedBy(DPSmall),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_report_sheet_header_expressive),
                    contentDescription = null,
                    modifier = Modifier
                        .width(132.dp)
                        .height(102.dp)
                )

                Text(
                    text = stringResource(R.string.report_post_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.report_post_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Text(
                        text = stringResource(R.string.report_post_post_id, postId),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLowest
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(DPSmall),
                verticalArrangement = Arrangement.spacedBy(DPSmall)
            ) {
                Text(
                    text = stringResource(R.string.report_post_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = DPMedium, top = DPSmall)
                )

                if (isTypesLoading && reportTypes.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = DPMedium),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (reportTypes.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(DPMedium),
                            verticalArrangement = Arrangement.spacedBy(DPSmall)
                        ) {
                            Text(
                                text = stringResource(R.string.report_post_types_error),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            FilledTonalButton(
                                onClick = viewModel::loadReportTypes,
                                enabled = !isTypesLoading && !isSubmitting
                            ) {
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp, max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(DPSmall)
                    ) {
                        items(reportTypes, key = { it }) { type ->
                            val isSelected = selectedType == type
                            val interactionSource = remember { MutableInteractionSource() }
                            val pressed by interactionSource.collectIsPressedAsState()
                            val scale by animateFloatAsState(
                                targetValue = if (pressed) 0.988f else 1f,
                                animationSpec = spring(),
                                label = "report_type_chip_scale"
                            )

                            FilterChip(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    },
                                selected = isSelected,
                                onClick = { viewModel.onSelectType(type) },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    null
                                },
                                label = {
                                    Text(
                                        reportTypeLabel(type = type),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = if (isSelected) TextAlign.Start else TextAlign.Center,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                },
                                shape = MaterialTheme.shapes.large,
                                interactionSource = interactionSource,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                                    selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                ),
                                elevation = FilterChipDefaults.filterChipElevation(
                                    elevation = if (isSelected) 3.dp else 0.dp,
                                    pressedElevation = if (isSelected) 4.dp else 1.dp
                                )
                            )
                        }
                    }
                }
            }
        }

        if (selectedType != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLowest
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(DPMedium),
                    verticalArrangement = Arrangement.spacedBy(DPSmall)
                ) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = description,
                        onValueChange = viewModel::onDescriptionChange,
                        enabled = !isSubmitting,
                        placeholder = {
                            Text(stringResource(R.string.report_post_optional_description))
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
                            text = stringResource(R.string.report_post_description_count, description.length),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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

        FilledTonalButton(
            onClick = { viewModel.submitReport(postId) },
            enabled = !isSubmitting && selectedType != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .width(18.dp)
                        .height(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.report_post_submitting))
            } else {
                Text(stringResource(R.string.report_post_confirm))
            }
        }
    }
}

@Composable
private fun reportTypeLabel(type: String): String {
    return when (type) {
        "scam" -> stringResource(R.string.report_type_scam)
        "unethical_content" -> stringResource(R.string.report_type_unethical_content)
        "violence" -> stringResource(R.string.report_type_violence)
        "spam" -> stringResource(R.string.report_type_spam)
        "false_information" -> stringResource(R.string.report_type_false_information)
        "copyright" -> stringResource(R.string.report_type_copyright)
        else -> type.replace("_", " ")
    }
}
