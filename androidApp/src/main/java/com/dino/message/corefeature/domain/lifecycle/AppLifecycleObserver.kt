package com.dino.message.corefeature.domain.lifecycle

fun interface AppLifecycleObserver {
    fun onAppLifecycleEvent(event: AppLifecycleEvent)
}
