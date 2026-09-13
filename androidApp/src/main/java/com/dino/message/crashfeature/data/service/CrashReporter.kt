package com.dino.message.crashfeature.data.service

import com.dino.message.corefeature.domain.logging.AppLogger
import com.dino.message.corefeature.data.remote.NetworkIssueReporter
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.domain.background.BackgroundTask
import com.dino.message.corefeature.domain.background.BackgroundTaskRegistry
import com.dino.message.corefeature.domain.background.BackgroundTaskScheduler
import com.dino.message.corefeature.domain.system.AppClock
import com.dino.message.corefeature.domain.system.AppProcessController
import com.dino.message.crashfeature.data.local.CrashLogDao
import com.dino.message.crashfeature.data.remote.CrashLogApiService
import com.dino.message.crashfeature.data.service.CrashLogBuilder.Companion.DEFAULT_BREADCRUMB_CATEGORY
import com.dino.message.crashfeature.data.service.CrashLogBuilder.Companion.MAX_BREADCRUMBS
import com.dino.message.crashfeature.data.service.CrashLogBuilder.Companion.MAX_BREADCRUMB_CATEGORY_LENGTH
import com.dino.message.crashfeature.data.service.CrashLogBuilder.Companion.MAX_BREADCRUMB_MESSAGE_LENGTH
import com.dino.message.crashfeature.data.service.CrashLogBuilder.Companion.MAX_EXCEPTION_MESSAGE_LENGTH
import com.dino.message.crashfeature.data.service.CrashLogBuilder.Companion.MAX_EXCEPTION_TYPE_LENGTH
import com.dino.message.crashfeature.data.service.CrashLogBuilder.Companion.MAX_STACK_TRACE_LENGTH
import com.dino.message.crashfeature.data.service.CrashLogBuilder.Companion.MAX_THREAD_NAME_LENGTH
import com.dino.message.crashfeature.domain.model.CrashBreadcrumb
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class CrashReporter constructor(
    private val crashLogDao: CrashLogDao,
    private val crashLogApiService: CrashLogApiService,
    private val sPrefManager: SPrefManager,
    private val backgroundTaskScheduler: BackgroundTaskScheduler,
    private val appClock: AppClock,
    private val appLogger: AppLogger,
    private val appProcessController: AppProcessController,
    private val crashLogBuilder: CrashLogBuilder,
    private val crashUiLauncher: CrashUiLauncher,
    backgroundTaskRegistry: BackgroundTaskRegistry,
) : NetworkIssueReporter {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isHandlerInstalled = AtomicBoolean(false)
    private val isFlushing = AtomicBoolean(false)
    private val breadcrumbLock = Any()
    private val breadcrumbs = ArrayDeque<CrashBreadcrumb>()

    init {
        backgroundTaskRegistry.register(BackgroundTask.CRASH_LOG_RETRY) {
            flushPendingLogsInternal()
        }
    }

    fun installGlobalExceptionHandler() {
        if (!isHandlerInstalled.compareAndSet(false, true)) return

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleUncaughtException(thread = thread, throwable = throwable)
        }
    }

    fun flushPendingLogsAsync() {
        backgroundTaskScheduler.schedule(BackgroundTask.CRASH_LOG_RETRY)
    }

    fun addBreadcrumb(
        message: String,
        category: String = DEFAULT_BREADCRUMB_CATEGORY,
    ) {
        val normalizedMessage = message.trim().trimTo(MAX_BREADCRUMB_MESSAGE_LENGTH)
        if (normalizedMessage.isBlank()) return
        val normalizedCategory = category.trim()
            .ifBlank { DEFAULT_BREADCRUMB_CATEGORY }
            .trimTo(MAX_BREADCRUMB_CATEGORY_LENGTH)

        synchronized(breadcrumbLock) {
            if (breadcrumbs.size >= MAX_BREADCRUMBS) {
                if (breadcrumbs.isNotEmpty()) {
                    breadcrumbs.removeFirst()
                }
            }
            breadcrumbs.addLast(
                CrashBreadcrumb(
                    timestampEpochMillis = appClock.currentTimeMillis(),
                    category = normalizedCategory,
                    message = normalizedMessage
                )
            )
        }
    }

    fun reportSocketOutboundQueueIssue(
        issueType: String,
        event: String,
        reason: String,
        attempts: Int,
        dedupeKey: String?,
    ) {
        val now = appClock.currentTimeMillis()
        val normalizedIssueType = issueType.trim().ifBlank { "SocketOutboundQueueIssue" }
        val normalizedEvent = event.trim().ifBlank { "unknown_event" }
        val normalizedReason = reason.trim().ifBlank { "unspecified" }
        val normalizedDedupeKey = dedupeKey?.trim()?.takeIf { it.isNotBlank() }
        addBreadcrumb(
            category = "socket_queue",
            message = "$normalizedIssueType event=$normalizedEvent reason=$normalizedReason attempts=$attempts"
        )

        val detailMessage = buildString {
            append("event=")
            append(normalizedEvent)
            append(" reason=")
            append(normalizedReason)
            append(" attempts=")
            append(attempts)
            if (normalizedDedupeKey != null) {
                append(" dedupe_key=")
                append(normalizedDedupeKey)
            }
        }.trimTo(MAX_EXCEPTION_MESSAGE_LENGTH)

        val stackTrace = buildString {
            append("issue_type=")
            append(normalizedIssueType)
            append('\n')
            append("event=")
            append(normalizedEvent)
            append('\n')
            append("reason=")
            append(normalizedReason)
            append('\n')
            append("attempts=")
            append(attempts)
            if (normalizedDedupeKey != null) {
                append('\n')
                append("dedupe_key=")
                append(normalizedDedupeKey)
            }
        }.trimTo(MAX_STACK_TRACE_LENGTH)

        val crashLog = crashLogBuilder.buildCrashLog(
            eventId = UUID.randomUUID().toString(),
            threadName = Thread.currentThread().name.ifBlank { "unknown" }.trimTo(MAX_THREAD_NAME_LENGTH),
            exceptionType = normalizedIssueType.trimTo(MAX_EXCEPTION_TYPE_LENGTH),
            exceptionMessage = detailMessage,
            stackTrace = stackTrace,
            username = sPrefManager.getUsername(),
            occurredAtEpochMillis = now,
            createdAtEpochMillis = now,
        )

        scope.launch {
            runCatching {
                crashLogDao.insert(crashLog)
                flushPendingLogsInternal(maxBatchSize = 5)
            }.onFailure { error ->
                appLogger.error(TAG, "Failed to persist socket queue issue log", error)
            }
        }
    }

    override fun reportNonFatalIssue(
        issueType: String,
        message: String,
        details: String?,
        category: String,
    ) {
        val now = appClock.currentTimeMillis()
        val normalizedIssueType = issueType.trim().ifBlank { "NonFatalIssue" }
        val normalizedMessage = message.trim().ifBlank { "unspecified" }
        val normalizedDetails = details
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.trimTo(MAX_STACK_TRACE_LENGTH)

        addBreadcrumb(
            category = category,
            message = "$normalizedIssueType message=$normalizedMessage"
        )

        val crashLog = crashLogBuilder.buildCrashLog(
            eventId = UUID.randomUUID().toString(),
            threadName = Thread.currentThread().name.ifBlank { "unknown" }.trimTo(MAX_THREAD_NAME_LENGTH),
            exceptionType = normalizedIssueType.trimTo(MAX_EXCEPTION_TYPE_LENGTH),
            exceptionMessage = normalizedMessage.trimTo(MAX_EXCEPTION_MESSAGE_LENGTH),
            stackTrace = (normalizedDetails ?: "no_details").trimTo(MAX_STACK_TRACE_LENGTH),
            username = sPrefManager.getUsername(),
            occurredAtEpochMillis = now,
            createdAtEpochMillis = now,
        )

        scope.launch {
            runCatching {
                crashLogDao.insert(crashLog)
                flushPendingLogsInternal(maxBatchSize = 5)
            }.onFailure { error ->
                appLogger.error(TAG, "Failed to persist non-fatal issue log", error)
            }
        }
    }

    private fun handleUncaughtException(thread: Thread, throwable: Throwable) {
        addBreadcrumb(
            category = "crash",
            message = "Unhandled exception type=${throwable.javaClass.name} thread=${thread.name}"
        )
        val now = appClock.currentTimeMillis()
        val crashLog = crashLogBuilder.buildThrowableCrashLog(
            eventId = UUID.randomUUID().toString(),
            threadName = thread.name,
            throwable = throwable,
            breadcrumbs = breadcrumbSnapshot(),
            username = sPrefManager.getUsername(),
            occurredAtEpochMillis = now,
            createdAtEpochMillis = now,
        )

        runCatching {
            runBlocking {
                withContext(Dispatchers.IO) {
                    crashLogDao.insert(crashLog)
                    withTimeoutOrNull(1_500) {
                        flushPendingLogsInternal(maxBatchSize = 1)
                    }
                }
            }
        }.onFailure { error ->
            appLogger.error(TAG, "Failed to persist or upload crash log", error)
        }

        crashUiLauncher.launchCrashReport(crashLog.eventId)

        try {
            Thread.sleep(250)
        } catch (_: InterruptedException) {
            // no-op
        }

        appProcessController.terminateCurrentProcess(10)
    }

    private suspend fun flushPendingLogsInternal(maxBatchSize: Int = 20) {
        if (!isFlushing.compareAndSet(false, true)) return

        try {
            while (true) {
                val pendingLogs = crashLogDao.getOldestPending(maxBatchSize)
                if (pendingLogs.isEmpty()) {
                    break
                }

                val sentIds = mutableListOf<Long>()
                var failedLogId: Long? = null

                for (pendingLog in pendingLogs) {
                    val sent = crashLogApiService.uploadCrashLog(
                        payload = crashLogBuilder.toUploadRequest(pendingLog),
                    )

                    if (sent) {
                        sentIds += pendingLog.id
                    } else {
                        failedLogId = pendingLog.id
                        break
                    }
                }

                if (sentIds.isNotEmpty()) {
                    crashLogDao.deleteByIds(sentIds)
                }

                if (failedLogId != null) {
                    crashLogDao.markFailed(
                        ids = listOf(failedLogId),
                        error = "upload_failed",
                    )
                    break
                }
            }
        } finally {
            isFlushing.set(false)
        }
    }

    private fun breadcrumbSnapshot(): List<CrashBreadcrumb> {
        return synchronized(breadcrumbLock) {
            breadcrumbs.toList()
        }
    }

    private fun String.trimTo(maxLength: Int): String {
        return if (length <= maxLength) this else take(maxLength)
    }

    companion object {
        private const val TAG = "CrashReporter"
    }
}
