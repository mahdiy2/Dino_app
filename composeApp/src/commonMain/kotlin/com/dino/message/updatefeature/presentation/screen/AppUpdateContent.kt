package com.dino.message.updatefeature.presentation.screen

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dino.message.composeapp.generated.resources.Res
import com.dino.message.composeapp.generated.resources.later
import com.dino.message.composeapp.generated.resources.update_open_browser_failed
import com.dino.message.composeapp.generated.resources.update_open_website
import com.dino.message.composeapp.generated.resources.update_opening_website
import org.jetbrains.compose.resources.stringResource

@Composable
fun AppUpdateContent(
    isForceUpdate: Boolean,
    title: String,
    message: String,
    onOpenUpdateWebsite: () -> Boolean,
    onContinueWithoutUpdate: () -> Unit,
) {
    val openingWebsiteMessage = stringResource(Res.string.update_opening_website)
    val openBrowserFailedMessage = stringResource(Res.string.update_open_browser_failed)
    val statusMessage = remember(message) { mutableStateOf(message) }

    fun openUpdateWebsite() {
        val opened = onOpenUpdateWebsite()
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
            Text(stringResource(Res.string.update_open_website))
        }

        if (!isForceUpdate) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onContinueWithoutUpdate,
            ) {
                Text(stringResource(Res.string.later))
            }
        }
    }
}
