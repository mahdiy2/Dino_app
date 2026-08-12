package com.dino.message.corefeature.data.system

import android.app.Application
import android.os.Build
import com.dino.message.corefeature.domain.system.PlatformInfo
import com.dino.message.corefeature.domain.system.PlatformInfoProvider

class AndroidPlatformInfoProvider : PlatformInfoProvider {
    override val currentProcessName: String?
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Application.getProcessName()
        } else {
            null
        }

    override fun getPlatformInfo(): PlatformInfo {
        return PlatformInfo(
            deviceModel = Build.MODEL.orEmpty().ifBlank { UNKNOWN },
            deviceManufacturer = Build.MANUFACTURER.orEmpty().ifBlank { UNKNOWN },
            osVersion = Build.VERSION.RELEASE.orEmpty().ifBlank { UNKNOWN },
            sdkInt = Build.VERSION.SDK_INT,
        )
    }

    private companion object {
        const val UNKNOWN = "unknown"
    }
}
