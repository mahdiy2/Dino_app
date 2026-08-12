package com.dino.message.corefeature.data.background

import com.dino.message.corefeature.domain.background.BackgroundTask
import com.dino.message.corefeature.domain.background.BackgroundTaskHandler
import com.dino.message.corefeature.domain.background.BackgroundTaskRegistry
import com.dino.message.corefeature.domain.background.BackgroundTaskScheduler
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppLifecycleBackgroundTaskScheduler(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : BackgroundTaskScheduler, BackgroundTaskRegistry {

    private val handlers = ConcurrentHashMap<BackgroundTask, BackgroundTaskHandler>()

    init {
        handlers[BackgroundTask.PRIVATE_CHAT_UPLOAD_RETRY] = BackgroundTaskHandler {}
        handlers[BackgroundTask.GROUP_UPLOAD_RETRY] = BackgroundTaskHandler {}
    }

    override fun register(task: BackgroundTask, handler: BackgroundTaskHandler) {
        handlers[task] = handler
    }

    override fun schedule(task: BackgroundTask) {
        val handler = handlers[task] ?: return
        scope.launch {
            handler.run()
        }
    }
}
