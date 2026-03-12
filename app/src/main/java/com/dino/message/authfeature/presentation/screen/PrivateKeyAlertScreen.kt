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
    var privateKey = remember {
        viewModel.getPrivateKey()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DPMedium, horizontal = DPExtraLarge)
    ) {

        item {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Private Key",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.size(DPMedium))
        }

        item {

            Text(
                text = "Please keep your Private key in a safe place! You will need it for future logins.",
                color = MaterialTheme.colorScheme.error
            )

            Spacer(Modifier.size(DPExtraLarge))

            if (privateKey.isNullOrEmpty()) {
                Text(
                    text = "Not found. An Error has occurred",
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
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_outline_copy),
                        contentDescription = null
                    )
                    Text(
                        privateKey ?: "An Error has occurred"
                    )
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
                        MaterialTheme.colorScheme.primaryContainer
                    else Color.Black,
                    contentColor = Color.White,
                    onClick = {
                        viewModel.navigateToLoginScreen()
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