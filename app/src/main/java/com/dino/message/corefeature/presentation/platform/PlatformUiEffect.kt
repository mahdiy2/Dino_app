package com.dino.message.corefeature.presentation.platform

interface PlatformUiEffect {
    fun showToast(message: String)
    fun copyPlainText(label: String, text: String)
    fun openUrl(url: String): Boolean
}
