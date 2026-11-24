package com.dino.message.corefeature.data.background

import com.dino.message.corefeature.domain.background.BackgroundTask
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLifecycleBackgroundTaskSchedulerTest {

    @Test
    fun schedule_delegatesCrashRetryToPersistentScheduler() {
        var enqueued = 0
        val scheduler = AppLifecycleBackgroundTaskScheduler(
            enqueueCrashRetry = { enqueued++ },
            scope = CoroutineScope(Dispatchers.Unconfined),
        )

        scheduler.schedule(BackgroundTask.CRASH_LOG_RETRY)

        assertEquals(1, enqueued)
    }

    @Test
    fun schedule_afterReplacingHandler_runsLatestHandler() = runBlocking {
        val completedValue = CompletableDeferred<String>()
        val scheduler = AppLifecycleBackgroundTaskScheduler(
            enqueueCrashRetry = {},
            scope = CoroutineScope(Dispatchers.Unconfined),
        )

        scheduler.register(BackgroundTask.SOCKET_OUTBOUND_RETRY) {
            completedValue.complete("first")
        }
        scheduler.register(BackgroundTask.SOCKET_OUTBOUND_RETRY) {
            completedValue.complete("second")
        }

        scheduler.schedule(BackgroundTask.SOCKET_OUTBOUND_RETRY)

        assertEquals("second", withTimeout(1_000) { completedValue.await() })
    }

    @Test
    fun schedule_forDeferredUploadTasks_isSafeWithoutRegisteredHandler() {
        val scheduler = AppLifecycleBackgroundTaskScheduler(
            enqueueCrashRetry = {},
            scope = CoroutineScope(Dispatchers.Unconfined),
        )

        scheduler.schedule(BackgroundTask.PRIVATE_CHAT_UPLOAD_RETRY)
        scheduler.schedule(BackgroundTask.GROUP_UPLOAD_RETRY)
    }
}
