package com.dino.message.spashscreen.data.remote

import com.dino.message.corefeature.data.remote.BaseRoute

sealed class SplashRoute(url: String) : BaseRoute(url) {
    object CheckAppVersion : SplashRoute("api/app/version-check")
}
