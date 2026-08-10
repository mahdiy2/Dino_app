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
    fun schedule_runsRegisteredHandlerForTask() = runBlocking {
        val completedTask = CompletableDeferred<BackgroundTask>()
        val scheduler = AppLifecycleBackgroundTaskScheduler(
            scope = CoroutineScope(Dispatchers.Unconfined),
        )

        scheduler.register(BackgroundTask.CRASH_LOG_RETRY) {
            completedTask.complete(BackgroundTask.CRASH_LOG_RETRY)
        }

        scheduler.schedule(BackgroundTask.CRASH_LOG_RETRY)

        assertEquals(BackgroundTask.CRASH_LOG_RETRY, withTimeout(1_000) { completedTask.await() })
    }

    @Test
    fun schedule_afterReplacingHandler_runsLatestHandler() = runBlocking {
        val completedValue = CompletableDeferred<String>()
        val scheduler = AppLifecycleBackgroundTaskScheduler(
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
            scope = CoroutineScope(Dispatchers.Unconfined),
        )

        scheduler.schedule(BackgroundTask.PRIVATE_CHAT_UPLOAD_RETRY)
        scheduler.schedule(BackgroundTask.GROUP_UPLOAD_RETRY)
    }
}
