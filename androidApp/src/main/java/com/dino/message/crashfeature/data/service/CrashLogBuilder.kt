package com.dino.message.crashfeature.data.service

import com.dino.message.corefeature.domain.config.AppConfig
import com.dino.message.corefeature.domain.system.PlatformInfoProvider
import com.dino.message.crashfeature.data.local.CrashLogEntity
import com.dino.message.crashfeature.data.remote.dto.CrashLogUploadRequest
import com.dino.message.crashfeature.domain.model.CrashBreadcrumb
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant

class CrashLogBuilder(
    private val appConfig: AppConfig,
    private val platformInfoProvider: PlatformInfoProvider,
) {
    fun buildThrowableCrashLog(
        eventId: String,
        threadName: String,
        throwable: Throwable,
        breadcrumbs: List<CrashBreadcrumb>,
        username: String?,
        occurredAtEpochMillis: Long,
        createdAtEpochMillis: Long,
    ): CrashLogEntity {
        return buildCrashLog(
            eventId = eventId,
            threadName = threadName.ifBlank { UNKNOWN }.trimTo(MAX_THREAD_NAME_LENGTH),
            exceptionType = throwable.javaClass.name.trimTo(MAX_EXCEPTION_TYPE_LENGTH),
            exceptionMessage = buildExceptionMessage(
                original = throwable.message,
                breadcrumbSection = buildBreadcrumbSection(breadcrumbs),
            ),
            stackTrace = throwable.asStackTraceString().trimTo(MAX_STACK_TRACE_LENGTH),
            username = username,
            occurredAtEpochMillis = occurredAtEpochMillis,
            createdAtEpochMillis = createdAtEpochMillis,
        )
    }

    fun buildCrashLog(
        eventId: String,
        threadName: String,
        exceptionType: String,
        exceptionMessage: String?,
        stackTrace: String,
        username: String?,
        occurredAtEpochMillis: Long,
        createdAtEpochMillis: Long,
    ): CrashLogEntity {
        val platformInfo = platformInfoProvider.getPlatformInfo()
        return CrashLogEntity(
            eventId = eventId,
            appVersion = appConfig.versionName,
            appVersionCode = appConfig.versionCode,
            threadName = threadName.ifBlank { UNKNOWN }.trimTo(MAX_THREAD_NAME_LENGTH),
            exceptionType = exceptionType.ifBlank { UNKNOWN }.trimTo(MAX_EXCEPTION_TYPE_LENGTH),
            exceptionMessage = exceptionMessage?.trimTo(MAX_EXCEPTION_MESSAGE_LENGTH),
            stackTrace = stackTrace.trimTo(MAX_STACK_TRACE_LENGTH),
            occurredAtEpochMillis = occurredAtEpochMillis,
            createdAtEpochMillis = createdAtEpochMillis,
            deviceModel = platformInfo.deviceModel.trimTo(MAX_DEVICE_FIELD_LENGTH),
            deviceManufacturer = platformInfo.deviceManufacturer.trimTo(MAX_DEVICE_FIELD_LENGTH),
            osVersion = platformInfo.osVersion.trimTo(MAX_DEVICE_FIELD_LENGTH),
            sdkInt = platformInfo.sdkInt,
            buildType = appConfig.buildType.trimTo(MAX_BUILD_TYPE_LENGTH),
            username = username?.trimTo(MAX_USERNAME_LENGTH),
        )
    }

    fun toUploadRequest(crashLog: CrashLogEntity): CrashLogUploadRequest {
        return CrashLogUploadRequest(
            eventId = crashLog.eventId,
            appVersion = crashLog.appVersion,
            appVersionCode = crashLog.appVersionCode,
            threadName = crashLog.threadName,
            exceptionType = crashLog.exceptionType,
            exceptionMessage = crashLog.exceptionMessage,
            stackTrace = crashLog.stackTrace,
            occurredAtIsoUtc = Instant.ofEpochMilli(crashLog.occurredAtEpochMillis).toString(),
            deviceModel = crashLog.deviceModel,
            deviceManufacturer = crashLog.deviceManufacturer,
            osVersion = crashLog.osVersion,
            sdkInt = crashLog.sdkInt,
            buildType = crashLog.buildType,
            username = crashLog.username,
        )
    }

    fun buildBreadcrumbSection(breadcrumbs: List<CrashBreadcrumb>): String? {
        if (breadcrumbs.isEmpty()) return null

        val builder = StringBuilder()
        for (breadcrumb in breadcrumbs.takeLast(MAX_BREADCRUMBS_IN_REPORT)) {
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

        val breadcrumbBlock = "Breadcrumbs:\n$breadcrumbSection"
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
        const val MAX_THREAD_NAME_LENGTH = 120
        const val MAX_EXCEPTION_TYPE_LENGTH = 255
        const val MAX_EXCEPTION_MESSAGE_LENGTH = 2_000
        const val MAX_STACK_TRACE_LENGTH = 64_000
        const val MAX_DEVICE_FIELD_LENGTH = 120
        const val MAX_BUILD_TYPE_LENGTH = 40
        const val MAX_USERNAME_LENGTH = 80
        const val MAX_BREADCRUMBS = 40
        const val MAX_BREADCRUMBS_IN_REPORT = 20
        const val MAX_BREADCRUMB_CATEGORY_LENGTH = 40
        const val MAX_BREADCRUMB_MESSAGE_LENGTH = 220
        const val MAX_BREADCRUMB_SECTION_LENGTH = 1_200
        const val DEFAULT_BREADCRUMB_CATEGORY = "app"
        private const val UNKNOWN = "unknown"
    }
}
