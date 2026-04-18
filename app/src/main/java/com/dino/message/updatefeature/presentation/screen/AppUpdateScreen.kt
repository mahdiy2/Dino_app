package com.dino.message.updatefeature.presentation.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dino.message.updatefeature.presentation.viewmodel.AppUpdateViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph

@Destination<RootGraph>
@Composable
fun AppUpdateScreen(
    isForceUpdate: Boolean,
    title: String,
    message: String,
    @Suppress("UNUSED_PARAMETER")
    downloadUrl: String,
) {
    val viewModel: AppUpdateViewModel = hiltViewModel()
    val context = androidx.compose.ui.platform.LocalContext.current

    val statusMessage = remember { mutableStateOf(message) }

    BackHandler(enabled = isForceUpdate) {
        // Force-update gate: keep the user on this screen.
    }

    fun openUpdateWebsite() {
        val opened = openUpdateInBrowser(context)
        statusMessage.value = if (opened) {
            "Opening update website in your browser..."
        } else {
            "Could not open browser. Please visit https://api.dinosocial.ir manually."
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
            Text("Open Update Website")
        }

        if (!isForceUpdate) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.continueWithoutUpdate() },
            ) {
                Text("Later")
            }
        }
    }
}

private fun openUpdateInBrowser(context: Context): Boolean {
    return try {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(UPDATE_WEBSITE_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(browserIntent)
        true
    } catch (_: Exception) {
        false
    }
}

private const val UPDATE_WEBSITE_URL = "https://api.dinosocial.ir"
