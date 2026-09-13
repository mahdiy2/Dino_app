package com.dino.message.crashfeature.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CrashLogUploadRequest(
    @SerialName("event_id")
    val eventId: String,
    @SerialName("app_version")
    val appVersion: String,
    @SerialName("app_version_code")
    val appVersionCode: Int,
    @SerialName("thread_name")
    val threadName: String,
    @SerialName("exception_type")
    val exceptionType: String,
    @SerialName("exception_message")
    val exceptionMessage: String? = null,
    @SerialName("stack_trace")
    val stackTrace: String,
    @SerialName("occurred_at")
    val occurredAtIsoUtc: String,
    @SerialName("device_model")
    val deviceModel: String,
    @SerialName("device_manufacturer")
    val deviceManufacturer: String,
    @SerialName("os_version")
    val osVersion: String,
    @SerialName("sdk_int")
    val sdkInt: Int,
    @SerialName("build_type")
    val buildType: String,
    @SerialName("username")
    val username: String? = null,
)
