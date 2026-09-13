package com.dino.message.profilefeature.presentation.screen

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dino.message.composeapp.generated.resources.Res
import com.dino.message.composeapp.generated.resources.cancel
import com.dino.message.composeapp.generated.resources.delete
import com.dino.message.composeapp.generated.resources.delete_post_body
import com.dino.message.composeapp.generated.resources.delete_post_title
import com.dino.message.composeapp.generated.resources.report_post_post_id
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import org.jetbrains.compose.resources.stringResource

@Composable
fun DeletePostBottomSheetContent(
    postId: Long,
    isDeleting: Boolean,
    error: String?,
    onClose: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
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
                text = stringResource(Res.string.delete_post_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(Res.string.delete_post_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(Res.string.report_post_post_id, postId),
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
                        text = error,
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
                    onClick = onClose
                ) {
                    Text(stringResource(Res.string.cancel))
                }

                FilledTonalButton(
                    modifier = Modifier.padding(start = 8.dp),
                    enabled = !isDeleting,
                    onClick = onConfirmDelete
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = stringResource(Res.string.delete),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
