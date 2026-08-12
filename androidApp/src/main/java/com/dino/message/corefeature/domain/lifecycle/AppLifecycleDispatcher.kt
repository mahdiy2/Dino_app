package com.dino.message.corefeature.domain.lifecycle

class AppLifecycleDispatcher {
    private val observers = linkedSetOf<AppLifecycleObserver>()

    fun addObserver(observer: AppLifecycleObserver) {
        observers += observer
    }

    fun removeObserver(observer: AppLifecycleObserver) {
        observers -= observer
    }

    fun dispatch(event: AppLifecycleEvent) {
        observers.toList().forEach { observer ->
            observer.onAppLifecycleEvent(event)
        }
    }
}
