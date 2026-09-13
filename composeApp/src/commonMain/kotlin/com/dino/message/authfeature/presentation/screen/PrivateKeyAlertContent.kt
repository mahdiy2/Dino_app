package com.dino.message.authfeature.presentation.screen

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dino.message.composeapp.generated.resources.Res
import com.dino.message.composeapp.generated.resources.back_to_register
import com.dino.message.composeapp.generated.resources.confirm_registration
import com.dino.message.composeapp.generated.resources.hide_key
import com.dino.message.composeapp.generated.resources.private_key_body
import com.dino.message.composeapp.generated.resources.private_key_copied
import com.dino.message.composeapp.generated.resources.private_key_copy_hint
import com.dino.message.composeapp.generated.resources.private_key_important_title
import com.dino.message.composeapp.generated.resources.private_key_not_found
import com.dino.message.composeapp.generated.resources.private_key_title
import com.dino.message.composeapp.generated.resources.show_key_to_verify
import com.dino.message.composeapp.generated.resources.try_again
import com.dino.message.corefeature.presentation.icons.AppIcons
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import org.jetbrains.compose.resources.stringResource

@Composable
fun PrivateKeyAlertContent(
    privateKey: String?,
    hasCopied: Boolean,
    showFullKey: Boolean,
    confirmErrorMessage: String?,
    isConfirmLoading: Boolean,
    onRetryPrivateKey: () -> Unit,
    onBackToRegister: () -> Unit,
    onCopyPrivateKey: () -> Unit,
    onToggleShowFullKey: () -> Unit,
    onContinue: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DPMedium, horizontal = DPExtraLarge)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        item {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.private_key_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.size(DPMedium))
        }

        item {
            Text(
                text = stringResource(Res.string.private_key_important_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.size(DPMedium))
            Text(
                text = stringResource(Res.string.private_key_body),
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.size(DPExtraLarge))

            if (privateKey.isNullOrEmpty()) {
                Text(
                    text = stringResource(Res.string.private_key_not_found),
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.size(DPExtraLarge))
                FilledTonalButton(onClick = onRetryPrivateKey) {
                    Text(stringResource(Res.string.try_again))
                }
                Spacer(Modifier.size(DPMedium))
                TextButton(onClick = onBackToRegister) {
                    Text(stringResource(Res.string.back_to_register))
                }
            } else {
                FilledTonalButton(
                    shape = MaterialTheme.shapes.extraLarge,
                    onClick = onCopyPrivateKey
                ) {
                    Icon(imageVector = AppIcons.ContentCopy, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    if (hasCopied) {
                        Text(stringResource(Res.string.private_key_copied))
                    } else {
                        Text(stringResource(Res.string.private_key_copy_hint))
                    }
                }

                if (hasCopied) {
                    Spacer(Modifier.size(DPMedium))
                    TextButton(onClick = onToggleShowFullKey) {
                        Text(
                            if (showFullKey) {
                                stringResource(Res.string.hide_key)
                            } else {
                                stringResource(Res.string.show_key_to_verify)
                            }
                        )
                    }
                    if (showFullKey) {
                        Text(
                            text = privateKey,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            if (!confirmErrorMessage.isNullOrBlank()) {
                Spacer(Modifier.size(DPMedium))
                Text(
                    text = confirmErrorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.size(DPExtraLarge))
                FloatingActionButton(
                    shape = CircleShape,
                    containerColor = if (isSystemInDarkTheme())
                        if (hasCopied && !privateKey.isNullOrEmpty()) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    else
                        if (hasCopied && !privateKey.isNullOrEmpty()) Color.Black
                        else Color.Gray,
                    contentColor = if (hasCopied && !privateKey.isNullOrEmpty()) Color.White else Color.LightGray,
                    onClick = onContinue
                ) {
                    if (isConfirmLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(
                            imageVector = AppIcons.KeyboardArrowRight,
                            contentDescription = stringResource(Res.string.confirm_registration),
                        )
                    }
                }
            }
        }
    }
}
