package com.dino.message.profilefeature.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dino.message.corefeature.presentation.viewmodel.rememberStateHolder
import com.dino.message.R
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.profilefeature.presentation.viewmodel.DeletePostBottomSheetViewModel

@Composable
fun DeletePostBottomSheetScreen(
    postId: Long,
) {
    val viewModel: DeletePostBottomSheetViewModel = rememberStateHolder(postId)
    val isDeleting by viewModel.isDeleting.collectAsState()
    val error by viewModel.error.collectAsState()

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DPMedium)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(DPMedium)
        ) {
            Text(
                text = stringResource(R.string.delete_post_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.delete_post_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.report_post_post_id, postId),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

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
                OutlinedButton(
                    enabled = !isDeleting,
                    onClick = viewModel::closeSheet
                ) {
                    Text(stringResource(R.string.cancel))
                }

                FilledTonalButton(
                    modifier = Modifier.padding(start = 8.dp),
                    enabled = !isDeleting,
                    onClick = { viewModel.confirmDelete(postId) }
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = stringResource(R.string.delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

}
