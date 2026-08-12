package com.dino.message.corefeature.data.util

import android.content.Context
import android.provider.Settings
import com.dino.message.corefeature.domain.device.DeviceInfoProvider

class DeviceIdProvider constructor(
    private val context: Context,
) : DeviceInfoProvider {
    override fun getDeviceId(): String {
        val raw = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        return raw?.trim().orEmpty().ifBlank { "unknown-android-id" }
    }
}
