package com.dino.message.corefeature.domain.background

fun interface BackgroundTaskHandler {
    suspend fun run()
}

interface BackgroundTaskRegistry {
    fun register(task: BackgroundTask, handler: BackgroundTaskHandler)
}
