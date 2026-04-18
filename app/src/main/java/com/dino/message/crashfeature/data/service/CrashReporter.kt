package com.dino.message.crashfeature.data.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.util.Log
import com.dino.message.BuildConfig
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.crashfeature.data.local.CrashLogDao
import com.dino.message.crashfeature.data.local.CrashLogEntity
import com.dino.message.crashfeature.data.remote.CrashLogApiService
import com.dino.message.crashfeature.data.remote.dto.CrashLogUploadRequest
import com.dino.message.crashfeature.presentation.activity.CrashReportActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.system.exitProcess

@Singleton
class CrashReporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crashLogDao: CrashLogDao,
    private val crashLogApiService: CrashLogApiService,
    private val sPrefManager: SPrefManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isHandlerInstalled = AtomicBoolean(false)
    private val isFlushing = AtomicBoolean(false)

    fun installGlobalExceptionHandler() {
        if (!isHandlerInstalled.compareAndSet(false, true)) return

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleUncaughtException(thread = thread, throwable = throwable)
        }
    }

    fun flushPendingLogsAsync() {
        scope.launch {
            flushPendingLogsInternal()
        }
    }

    private fun handleUncaughtException(thread: Thread, throwable: Throwable) {
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
        return CrashLogEntity(
            eventId = UUID.randomUUID().toString(),
            appVersion = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE,
            threadName = thread.name.ifBlank { "unknown" }.trimTo(MAX_THREAD_NAME_LENGTH),
            exceptionType = throwable.javaClass.name.trimTo(MAX_EXCEPTION_TYPE_LENGTH),
            exceptionMessage = throwable.message?.trimTo(MAX_EXCEPTION_MESSAGE_LENGTH),
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
    }
}
