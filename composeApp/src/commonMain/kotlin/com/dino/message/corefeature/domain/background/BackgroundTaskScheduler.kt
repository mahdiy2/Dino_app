package com.dino.message.corefeature.domain.background

interface BackgroundTaskScheduler {
    fun schedule(task: BackgroundTask)
}
