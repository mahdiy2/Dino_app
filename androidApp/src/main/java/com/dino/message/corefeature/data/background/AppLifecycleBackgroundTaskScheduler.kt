package com.dino.message.corefeature.data.background

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.dino.message.corefeature.domain.background.BackgroundTask
import com.dino.message.corefeature.domain.background.BackgroundTaskHandler
import com.dino.message.corefeature.domain.background.BackgroundTaskRegistry
import com.dino.message.corefeature.domain.background.BackgroundTaskScheduler
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppLifecycleBackgroundTaskScheduler(
    private val enqueueCrashRetry: () -> Unit,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : BackgroundTaskScheduler, BackgroundTaskRegistry {

    constructor(context: Context) : this(enqueueCrashRetry = {
        val request = OneTimeWorkRequestBuilder<CrashLogRetryWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("crash-log-retry", ExistingWorkPolicy.KEEP, request)
    })

    private val handlers = ConcurrentHashMap<BackgroundTask, BackgroundTaskHandler>()

    override fun register(task: BackgroundTask, handler: BackgroundTaskHandler) {
        handlers[task] = handler
    }

    override fun schedule(task: BackgroundTask) {
        if (task == BackgroundTask.CRASH_LOG_RETRY) {
            enqueueCrashRetry()
            return
        }
        val handler = handlers[task] ?: return
        scope.launch {
            handler.run()
        }
    }
}
