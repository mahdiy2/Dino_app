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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dino.message.R
import com.dino.message.authfeature.presentation.viewmodel.PrivateKeyAlertScreenViewModel
import com.dino.message.corefeature.presentation.ui.theme.DPExtraLarge
import com.dino.message.corefeature.presentation.ui.theme.DPMedium
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.bottomsheet.spec.DestinationStyleBottomSheet

@Destination<RootGraph>(style = DestinationStyleBottomSheet::class)
@Composable
fun PrivateKeyAlertScreen() {
    val viewModel: PrivateKeyAlertScreenViewModel = hiltViewModel()
    val clipboardManager = LocalClipboardManager.current
    var privateKey by remember { mutableStateOf(viewModel.getPrivateKey()) }
    var hasCopied by rememberSaveable { mutableStateOf(false) }
    var showFullKey by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DPMedium, horizontal = DPExtraLarge)
    ) {

        item {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "🔑 Private Key",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.size(DPMedium))
        }

        item {

            Text(
                text = "⚠️ IMPORTANT: Save this key now!",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(Modifier.size(DPMedium))

            Text(
                text = "This is your encryption key. Without it, you will NOT be able to read messages on another device or after re-installing the app.\n\n" +
                        "• Tap the button below to copy it\n" +
                        "• Paste it somewhere safe (notes app, email to yourself, etc.)\n" +
                        "• You will need to paste it when logging in on a new device",
                color = MaterialTheme.colorScheme.error
            )

            Spacer(Modifier.size(DPExtraLarge))

            if (privateKey.isNullOrEmpty()) {
                Text(
                    text = "Not found. An error has occurred.",
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(Modifier.size(DPExtraLarge))

                FilledTonalButton(
                    onClick = {
                        privateKey = viewModel.getPrivateKey()
                    }
                ) {
                    Text("Try again!")
                }
            } else {
                FilledTonalButton(
                    shape = MaterialTheme.shapes.extraLarge,
                    onClick = {
                        clipboardManager.setText(AnnotatedString(privateKey ?: "An Error has occurred"))
                        hasCopied = true
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_outline_copy),
                        contentDescription = null
                    )
                    Spacer(Modifier.size(8.dp))
                    if (hasCopied) {
                        Text("✅ Copied! Save it somewhere safe.")
                    } else {
                        Text("📋 Tap to copy your private key")
                    }
                }

                if (hasCopied) {
                    Spacer(Modifier.size(DPMedium))
                    TextButton(onClick = { showFullKey = !showFullKey }) {
                        Text(if (showFullKey) "Hide key" else "Show key to verify")
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
                        if (hasCopied || privateKey.isNullOrEmpty()) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    else
                        if (hasCopied || privateKey.isNullOrEmpty()) Color.Black
                        else Color.Gray,
                    contentColor = if (hasCopied || privateKey.isNullOrEmpty()) Color.White else Color.LightGray,
                    onClick = {
                        if (hasCopied || privateKey.isNullOrEmpty()) {
                            viewModel.navigateToLoginScreen()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Go to Login",
                    )

                }
            }

        }

    }
}
