package com.dino.message.corefeature.domain.startup

import com.dino.message.corefeature.domain.lifecycle.AppLifecycleDispatcher
import com.dino.message.corefeature.domain.lifecycle.AppLifecycleEvent
import com.dino.message.corefeature.domain.lifecycle.AppLifecycleObserver

class AppStartupCoordinator(
    private val lifecycleDispatcher: AppLifecycleDispatcher,
    private val backgroundCleanupObserver: AppLifecycleObserver,
) {
    private var initialized = false

    fun initializeCommonState() {
        if (initialized) return

        lifecycleDispatcher.addObserver(backgroundCleanupObserver)
        lifecycleDispatcher.dispatch(AppLifecycleEvent.ApplicationStarted)
        initialized = true
    }
}
