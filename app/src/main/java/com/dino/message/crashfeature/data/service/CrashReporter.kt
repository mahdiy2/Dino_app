package com.dino.message.crashfeature.data.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.Log
import com.dino.message.BuildConfig
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.domain.background.BackgroundTask
import com.dino.message.corefeature.domain.background.BackgroundTaskRegistry
import com.dino.message.corefeature.domain.background.BackgroundTaskScheduler
import com.dino.message.crashfeature.data.local.CrashLogDao
import com.dino.message.crashfeature.data.local.CrashLogEntity
import com.dino.message.crashfeature.data.remote.CrashLogApiService
import com.dino.message.crashfeature.data.remote.dto.CrashLogUploadRequest
import com.dino.message.crashfeature.presentation.activity.CrashReportActivity
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.system.exitProcess

class CrashReporter constructor(
    private val context: Context,
    private val crashLogDao: CrashLogDao,
    private val crashLogApiService: CrashLogApiService,
    private val sPrefManager: SPrefManager,
    private val backgroundTaskScheduler: BackgroundTaskScheduler,
    backgroundTaskRegistry: BackgroundTaskRegistry,
) {
    private data class CrashBreadcrumb(
        val timestampEpochMillis: Long,
        val category: String,
        val message: String,
    )

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
                    timestampEpochMillis = System.currentTimeMillis(),
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
        val now = System.currentTimeMillis()
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

        val crashLog = CrashLogEntity(
            eventId = UUID.randomUUID().toString(),
            appVersion = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE,
            threadName = Thread.currentThread().name.ifBlank { "unknown" }.trimTo(MAX_THREAD_NAME_LENGTH),
            exceptionType = normalizedIssueType.trimTo(MAX_EXCEPTION_TYPE_LENGTH),
            exceptionMessage = detailMessage,
            stackTrace = stackTrace,
            occurredAtEpochMillis = now,
            createdAtEpochMillis = now,
            deviceModel = Build.MODEL.orEmpty().ifBlank { "unknown" }.trimTo(MAX_DEVICE_FIELD_LENGTH),
            deviceManufacturer = Build.MANUFACTURER.orEmpty()
                .ifBlank { "unknown" }
                .trimTo(MAX_DEVICE_FIELD_LENGTH),
            osVersion = Build.VERSION.RELEASE.orEmpty()
                .ifBlank { "unknown" }
                .trimTo(MAX_DEVICE_FIELD_LENGTH),
            sdkInt = Build.VERSION.SDK_INT,
            buildType = BuildConfig.BUILD_TYPE.trimTo(MAX_BUILD_TYPE_LENGTH),
            username = sPrefManager.getUsername()?.trimTo(MAX_USERNAME_LENGTH),
        )

        scope.launch {
            runCatching {
                crashLogDao.insert(crashLog)
                flushPendingLogsInternal(maxBatchSize = 5)
            }.onFailure { error ->
                Log.e(TAG, "Failed to persist socket queue issue log", error)
            }
        }
    }

    fun reportNonFatalIssue(
        issueType: String,
        message: String,
        details: String? = null,
        category: String = DEFAULT_BREADCRUMB_CATEGORY,
    ) {
        val now = System.currentTimeMillis()
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

        val crashLog = CrashLogEntity(
            eventId = UUID.randomUUID().toString(),
            appVersion = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE,
            threadName = Thread.currentThread().name.ifBlank { "unknown" }.trimTo(MAX_THREAD_NAME_LENGTH),
            exceptionType = normalizedIssueType.trimTo(MAX_EXCEPTION_TYPE_LENGTH),
            exceptionMessage = normalizedMessage.trimTo(MAX_EXCEPTION_MESSAGE_LENGTH),
            stackTrace = (normalizedDetails ?: "no_details").trimTo(MAX_STACK_TRACE_LENGTH),
            occurredAtEpochMillis = now,
            createdAtEpochMillis = now,
            deviceModel = Build.MODEL.orEmpty().ifBlank { "unknown" }.trimTo(MAX_DEVICE_FIELD_LENGTH),
            deviceManufacturer = Build.MANUFACTURER.orEmpty()
                .ifBlank { "unknown" }
                .trimTo(MAX_DEVICE_FIELD_LENGTH),
            osVersion = Build.VERSION.RELEASE.orEmpty()
                .ifBlank { "unknown" }
                .trimTo(MAX_DEVICE_FIELD_LENGTH),
            sdkInt = Build.VERSION.SDK_INT,
            buildType = BuildConfig.BUILD_TYPE.trimTo(MAX_BUILD_TYPE_LENGTH),
            username = sPrefManager.getUsername()?.trimTo(MAX_USERNAME_LENGTH),
        )

        scope.launch {
            runCatching {
                crashLogDao.insert(crashLog)
                flushPendingLogsInternal(maxBatchSize = 5)
            }.onFailure { error ->
                Log.e(TAG, "Failed to persist non-fatal issue log", error)
            }
        }
    }

    private fun handleUncaughtException(thread: Thread, throwable: Throwable) {
        addBreadcrumb(
            category = "crash",
            message = "Unhandled exception type=${throwable.javaClass.name} thread=${thread.name}"
        )
        val crashLog = buildCrashLogEntity(thread = thread, throwable = throwable)

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
            Log.e(TAG, "Failed to persist or upload crash log", error)
        }

        launchCrashReportActivity(crashLog.eventId)

        try {
            Thread.sleep(250)
        } catch (_: InterruptedException) {
            // no-op
        }

        Process.killProcess(Process.myPid())
        exitProcess(10)
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
                        payload = pendingLog.toUploadRequest(),
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

    private fun launchCrashReportActivity(eventId: String) {
        val intent = Intent(context, CrashReportActivity::class.java).apply {
            putExtra(CrashReportActivity.EXTRA_EVENT_ID, eventId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        runCatching {
            context.startActivity(intent)
        }.onFailure { error ->
            Log.e(TAG, "Unable to launch crash report activity", error)
        }
    }

    private fun buildCrashLogEntity(thread: Thread, throwable: Throwable): CrashLogEntity {
        val now = System.currentTimeMillis()
        val breadcrumbSection = buildBreadcrumbSection()
        return CrashLogEntity(
            eventId = UUID.randomUUID().toString(),
            appVersion = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE,
            threadName = thread.name.ifBlank { "unknown" }.trimTo(MAX_THREAD_NAME_LENGTH),
            exceptionType = throwable.javaClass.name.trimTo(MAX_EXCEPTION_TYPE_LENGTH),
            exceptionMessage = buildExceptionMessage(
                original = throwable.message,
                breadcrumbSection = breadcrumbSection
            ),
            stackTrace = throwable.asStackTraceString().trimTo(MAX_STACK_TRACE_LENGTH),
            occurredAtEpochMillis = now,
            createdAtEpochMillis = now,
            deviceModel = Build.MODEL.orEmpty().ifBlank { "unknown" }.trimTo(MAX_DEVICE_FIELD_LENGTH),
            deviceManufacturer = Build.MANUFACTURER.orEmpty()
                .ifBlank { "unknown" }
                .trimTo(MAX_DEVICE_FIELD_LENGTH),
            osVersion = Build.VERSION.RELEASE.orEmpty()
                .ifBlank { "unknown" }
                .trimTo(MAX_DEVICE_FIELD_LENGTH),
            sdkInt = Build.VERSION.SDK_INT,
            buildType = BuildConfig.BUILD_TYPE.trimTo(MAX_BUILD_TYPE_LENGTH),
            username = sPrefManager.getUsername()?.trimTo(MAX_USERNAME_LENGTH),
        )
    }

    private fun buildBreadcrumbSection(): String? {
        val snapshot = synchronized(breadcrumbLock) {
            if (breadcrumbs.isEmpty()) return null
            breadcrumbs.takeLast(MAX_BREADCRUMBS_IN_REPORT)
        }

        val builder = StringBuilder()
        for (breadcrumb in snapshot) {
            val line = buildString {
                append(Instant.ofEpochMilli(breadcrumb.timestampEpochMillis).toString())
                append(" [")
                append(breadcrumb.category)
                append("] ")
                append(breadcrumb.message)
            }

            val additionalLength = if (builder.isEmpty()) line.length else line.length + 1
            if (builder.length + additionalLength > MAX_BREADCRUMB_SECTION_LENGTH) {
                break
            }
            if (builder.isNotEmpty()) {
                builder.append('\n')
            }
            builder.append(line)
        }

        return builder.toString().takeIf { it.isNotBlank() }
    }

    private fun buildExceptionMessage(
        original: String?,
        breadcrumbSection: String?,
    ): String? {
        val normalizedOriginal = original?.trim()?.takeIf { it.isNotBlank() }
        if (breadcrumbSection.isNullOrBlank()) {
            return normalizedOriginal?.trimTo(MAX_EXCEPTION_MESSAGE_LENGTH)
        }

        val breadcrumbHeader = "Breadcrumbs:"
        val breadcrumbBlock = "$breadcrumbHeader\n$breadcrumbSection"

        if (normalizedOriginal.isNullOrBlank()) {
            return breadcrumbBlock.trimTo(MAX_EXCEPTION_MESSAGE_LENGTH)
        }

        val separator = "\n\n"
        val maxOriginalLength = (
            MAX_EXCEPTION_MESSAGE_LENGTH -
                separator.length -
                breadcrumbBlock.length
            ).coerceAtLeast(0)
        val trimmedOriginal = normalizedOriginal.trimTo(maxOriginalLength)
        if (trimmedOriginal.isBlank()) {
            return breadcrumbBlock.trimTo(MAX_EXCEPTION_MESSAGE_LENGTH)
        }
        return "$trimmedOriginal$separator$breadcrumbBlock".trimTo(MAX_EXCEPTION_MESSAGE_LENGTH)
    }

    private fun CrashLogEntity.toUploadRequest(): CrashLogUploadRequest {
        return CrashLogUploadRequest(
            eventId = eventId,
            appVersion = appVersion,
            appVersionCode = appVersionCode,
            threadName = threadName,
            exceptionType = exceptionType,
            exceptionMessage = exceptionMessage,
            stackTrace = stackTrace,
            occurredAtIsoUtc = Instant.ofEpochMilli(occurredAtEpochMillis).toString(),
            deviceModel = deviceModel,
            deviceManufacturer = deviceManufacturer,
            osVersion = osVersion,
            sdkInt = sdkInt,
            buildType = buildType,
            username = username,
        )
    }

    private fun Throwable.asStackTraceString(): String {
        val writer = StringWriter()
        val printWriter = PrintWriter(writer)
        printStackTrace(printWriter)
        printWriter.flush()
        return writer.toString()
    }

    private fun String.trimTo(maxLength: Int): String {
        return if (length <= maxLength) this else take(maxLength)
    }

    companion object {
        private const val TAG = "CrashReporter"
        private const val MAX_THREAD_NAME_LENGTH = 120
        private const val MAX_EXCEPTION_TYPE_LENGTH = 255
        private const val MAX_EXCEPTION_MESSAGE_LENGTH = 2_000
        private const val MAX_STACK_TRACE_LENGTH = 64_000
        private const val MAX_DEVICE_FIELD_LENGTH = 120
        private const val MAX_BUILD_TYPE_LENGTH = 40
        private const val MAX_USERNAME_LENGTH = 80
        private const val MAX_BREADCRUMBS = 40
        private const val MAX_BREADCRUMBS_IN_REPORT = 20
        private const val MAX_BREADCRUMB_CATEGORY_LENGTH = 40
        private const val MAX_BREADCRUMB_MESSAGE_LENGTH = 220
        private const val MAX_BREADCRUMB_SECTION_LENGTH = 1_200
        private const val DEFAULT_BREADCRUMB_CATEGORY = "app"
    }
}
