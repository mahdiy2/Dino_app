package com.dino.message.corefeature.domain.lifecycle

sealed class AppLifecycleEvent {
    data class ScreenStarted(val screenName: String, val activeScreenCount: Int) : AppLifecycleEvent()
    data class ScreenStopped(val screenName: String, val activeScreenCount: Int) : AppLifecycleEvent()
    data class ScreenVisible(val screenName: String) : AppLifecycleEvent()
    object EnteredBackground : AppLifecycleEvent()
}
