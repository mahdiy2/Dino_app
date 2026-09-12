package com.dino.message.authfeature.presentation.screen

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dino.message.corefeature.presentation.viewmodel.rememberStateHolder
import com.dino.message.R
import com.dino.message.authfeature.presentation.viewmodel.PrivateKeyAlertScreenViewModel
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.dino.message.corefeature.presentation.icons.AppIcons

@Composable
fun PrivateKeyAlertScreen() {
    val viewModel: PrivateKeyAlertScreenViewModel = rememberStateHolder()
    val clipboardManager = LocalClipboardManager.current
    val confirmErrorMessage by viewModel.confirmErrorMessage
    val isConfirmLoading by viewModel.confirmLoadingState
    var privateKey by remember { mutableStateOf(viewModel.getPendingPrivateKey()) }
    var hasCopied by rememberSaveable { mutableStateOf(false) }
    var showFullKey by rememberSaveable { mutableStateOf(false) }
    val genericError = stringResource(R.string.an_error_occurred)

    BackHandler(enabled = true) {
        // Block back navigation so the user cannot bypass key backup confirmation.
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DPMedium, horizontal = DPExtraLarge)
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {

        item {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.private_key_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.size(DPMedium))
        }

        item {

            Text(
                text = stringResource(R.string.private_key_important_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(Modifier.size(DPMedium))

            Text(
                text = stringResource(R.string.private_key_body),
                color = MaterialTheme.colorScheme.error
            )

            Spacer(Modifier.size(DPExtraLarge))

            if (privateKey.isNullOrEmpty()) {
                Text(
                    text = stringResource(R.string.private_key_not_found),
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(Modifier.size(DPExtraLarge))

                FilledTonalButton(
                    onClick = {
                        privateKey = viewModel.getPendingPrivateKey()
                    }
                ) {
                    Text(stringResource(R.string.try_again))
                }

                Spacer(Modifier.size(DPMedium))

                TextButton(
                    onClick = { viewModel.navigateToRegisterScreenAndResetPending() }
                ) {
                    Text(stringResource(R.string.back_to_register))
                }
            } else {
                FilledTonalButton(
                    shape = MaterialTheme.shapes.extraLarge,
                    onClick = {
                        clipboardManager.setText(AnnotatedString(privateKey ?: genericError))
                        hasCopied = true
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_outline_copy),
                        contentDescription = null
                    )
                    Spacer(Modifier.size(8.dp))
                    if (hasCopied) {
                        Text(stringResource(R.string.private_key_copied))
                    } else {
                        Text(stringResource(R.string.private_key_copy_hint))
                    }
                }

                if (hasCopied) {
                    Spacer(Modifier.size(DPMedium))
                    TextButton(onClick = { showFullKey = !showFullKey }) {
                        Text(
                            if (showFullKey) {
                                stringResource(R.string.hide_key)
                            } else {
                                stringResource(R.string.show_key_to_verify)
                            }
                        )
                    }
                    if (showFullKey) {
                        Text(
                            text = privateKey.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            if (!confirmErrorMessage.isNullOrBlank()) {
                Spacer(Modifier.size(DPMedium))
                Text(
                    text = confirmErrorMessage.orEmpty(),
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
                Spacer(
                    Modifier.size(DPExtraLarge)
                )

                FloatingActionButton(
                    shape = CircleShape,
                    containerColor = if (isSystemInDarkTheme())
                        if (hasCopied && !privateKey.isNullOrEmpty()) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    else
                        if (hasCopied && !privateKey.isNullOrEmpty()) Color.Black
                        else Color.Gray,
                    contentColor = if (hasCopied && !privateKey.isNullOrEmpty()) Color.White else Color.LightGray,
                    onClick = {
                        viewModel.onContinueClicked(hasCopied = hasCopied)
                    }
                ) {
                    if (isConfirmLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Icon(
                            imageVector = AppIcons.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.confirm_registration),
                        )
                    }

                }
            }

        }

    }
}
