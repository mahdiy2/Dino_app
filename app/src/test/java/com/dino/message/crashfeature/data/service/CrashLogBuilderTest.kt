package com.dino.message.crashfeature.data.service

import com.dino.message.corefeature.domain.config.AppConfig
import com.dino.message.corefeature.domain.system.PlatformInfo
import com.dino.message.corefeature.domain.system.PlatformInfoProvider
import com.dino.message.crashfeature.domain.model.CrashBreadcrumb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashLogBuilderTest {
    private val builder = CrashLogBuilder(
        appConfig = FakeAppConfig,
        platformInfoProvider = FakePlatformInfoProvider,
    )

    @Test
    fun buildCrashLogIncludesAppPlatformAndUserMetadata() {
        val log = builder.buildCrashLog(
            eventId = "event-1",
            threadName = "main",
            exceptionType = "TestException",
            exceptionMessage = "message",
            stackTrace = "stack",
            username = "mahdi",
            occurredAtEpochMillis = 1_700_000_000_000,
            createdAtEpochMillis = 1_700_000_000_001,
        )

        assertEquals("event-1", log.eventId)
        assertEquals("1.2.3", log.appVersion)
        assertEquals(123, log.appVersionCode)
        assertEquals("debug", log.buildType)
        assertEquals("Pixel", log.deviceModel)
        assertEquals("Google", log.deviceManufacturer)
        assertEquals("14", log.osVersion)
        assertEquals(34, log.sdkInt)
        assertEquals("mahdi", log.username)
    }

    @Test
    fun buildThrowableCrashLogAddsBreadcrumbsToExceptionMessage() {
        val log = builder.buildThrowableCrashLog(
            eventId = "event-2",
            threadName = "worker",
            throwable = IllegalStateException("boom"),
            breadcrumbs = listOf(
                CrashBreadcrumb(
                    timestampEpochMillis = 0,
                    category = "lifecycle",
                    message = "Application created",
                ),
            ),
            username = null,
            occurredAtEpochMillis = 1_000,
            createdAtEpochMillis = 1_000,
        )

        val message = log.exceptionMessage.orEmpty()
        assertTrue(message.contains("boom"))
        assertTrue(message.contains("Breadcrumbs:"))
        assertTrue(message.contains("1970-01-01T00:00:00Z [lifecycle] Application created"))
        assertTrue(log.stackTrace.contains("IllegalStateException"))
    }

    @Test
    fun buildCrashLogTrimsLongFields() {
        val longText = "x".repeat(CrashLogBuilder.MAX_STACK_TRACE_LENGTH + 10)

        val log = builder.buildCrashLog(
            eventId = "event-3",
            threadName = "t".repeat(CrashLogBuilder.MAX_THREAD_NAME_LENGTH + 10),
            exceptionType = "e".repeat(CrashLogBuilder.MAX_EXCEPTION_TYPE_LENGTH + 10),
            exceptionMessage = "m".repeat(CrashLogBuilder.MAX_EXCEPTION_MESSAGE_LENGTH + 10),
            stackTrace = longText,
            username = "u".repeat(CrashLogBuilder.MAX_USERNAME_LENGTH + 10),
            occurredAtEpochMillis = 1,
            createdAtEpochMillis = 1,
        )

        assertEquals(CrashLogBuilder.MAX_THREAD_NAME_LENGTH, log.threadName.length)
        assertEquals(CrashLogBuilder.MAX_EXCEPTION_TYPE_LENGTH, log.exceptionType.length)
        assertEquals(CrashLogBuilder.MAX_EXCEPTION_MESSAGE_LENGTH, log.exceptionMessage?.length)
        assertEquals(CrashLogBuilder.MAX_STACK_TRACE_LENGTH, log.stackTrace.length)
        assertEquals(CrashLogBuilder.MAX_USERNAME_LENGTH, log.username?.length)
    }

    @Test
    fun toUploadRequestMapsOccurredAtToIsoUtc() {
        val log = builder.buildCrashLog(
            eventId = "event-4",
            threadName = "main",
            exceptionType = "TestException",
            exceptionMessage = null,
            stackTrace = "stack",
            username = null,
            occurredAtEpochMillis = 1_700_000_000_000,
            createdAtEpochMillis = 1_700_000_000_100,
        )

        val request = builder.toUploadRequest(log)

        assertEquals("event-4", request.eventId)
        assertEquals("2023-11-14T22:13:20Z", request.occurredAtIsoUtc)
        assertEquals(log.deviceModel, request.deviceModel)
        assertEquals(log.stackTrace, request.stackTrace)
    }

    private object FakeAppConfig : AppConfig {
        override val baseUrl: String = "https://example.invalid/"
        override val isDebug: Boolean = true
        override val versionName: String = "1.2.3"
        override val versionCode: Int = 123
        override val buildType: String = "debug"
        override val authPasswordRequireSymbol: Boolean = false
    }

    private object FakePlatformInfoProvider : PlatformInfoProvider {
        override val currentProcessName: String? = "test"

        override fun getPlatformInfo(): PlatformInfo {
            return PlatformInfo(
                deviceModel = "Pixel",
                deviceManufacturer = "Google",
                osVersion = "14",
                sdkInt = 34,
            )
        }
    }
}
