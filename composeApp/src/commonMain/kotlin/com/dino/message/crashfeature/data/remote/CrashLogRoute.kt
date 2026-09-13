package com.dino.message.crashfeature.data.remote

import com.dino.message.corefeature.data.remote.BaseRoute

sealed class CrashLogRoute(url: String) : BaseRoute(url) {
    object UploadCrashLog : CrashLogRoute("api/crash-logs")
}
