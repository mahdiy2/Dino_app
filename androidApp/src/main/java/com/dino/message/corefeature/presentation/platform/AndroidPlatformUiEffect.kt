package com.dino.message.corefeature.presentation.platform

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

class AndroidPlatformUiEffect(
    private val context: Context,
) : PlatformUiEffect {

    override fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun copyPlainText(label: String, text: String) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE)
            as android.content.ClipboardManager
        clipboardManager.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    override fun openUrl(url: String): Boolean {
        return try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
            true
        } catch (_: Exception) {
            false
        }
    }
}

class AndroidPlatformBackHandler : PlatformBackHandler {
    @Composable
    override fun Handle(enabled: Boolean, onBack: () -> Unit) {
        BackHandler(enabled = enabled, onBack = onBack)
    }
}

@Composable
fun rememberAndroidPlatformUiEffect(): PlatformUiEffect {
    val context = LocalContext.current
    return remember(context) { AndroidPlatformUiEffect(context) }
}

@Composable
fun rememberAndroidPlatformBackHandler(): PlatformBackHandler {
    return remember { AndroidPlatformBackHandler() }
}
