package com.dino.message.updatefeature.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import com.dino.message.R
import com.dino.message.corefeature.presentation.platform.LocalPlatformUiEffect
import com.dino.message.updatefeature.presentation.viewmodel.AppUpdateViewModel

@Composable
fun AppUpdateScreen(
    isForceUpdate: Boolean,
    title: String,
    message: String,
    @Suppress("UNUSED_PARAMETER")
    downloadUrl: String,
) {
    val viewModel: AppUpdateViewModel = koinViewModel()
    val platformUiEffect = LocalPlatformUiEffect.current
    val openingWebsiteMessage = stringResource(R.string.update_opening_website)
    val openBrowserFailedMessage = stringResource(R.string.update_open_browser_failed)

    val statusMessage = remember { mutableStateOf(message) }

    BackHandler(enabled = isForceUpdate) {
        // Force-update gate: keep the user on this screen.
    }

    fun openUpdateWebsite() {
        val opened = platformUiEffect.openUrl(UPDATE_WEBSITE_URL)
        statusMessage.value = if (opened) {
            openingWebsiteMessage
        } else {
            openBrowserFailedMessage
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = statusMessage.value,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(20.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = { openUpdateWebsite() },
        ) {
            Text(stringResource(R.string.update_open_website))
        }

        if (!isForceUpdate) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.continueWithoutUpdate() },
            ) {
                Text(stringResource(R.string.later))
            }
        }
    }
}

private const val UPDATE_WEBSITE_URL = "https://api.dinosocial.ir"
