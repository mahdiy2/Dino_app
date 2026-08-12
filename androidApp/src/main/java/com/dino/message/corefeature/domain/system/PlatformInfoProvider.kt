package com.dino.message.corefeature.domain.system

data class PlatformInfo(
    val deviceModel: String,
    val deviceManufacturer: String,
    val osVersion: String,
    val sdkInt: Int,
)

interface PlatformInfoProvider {
    val currentProcessName: String?
    fun getPlatformInfo(): PlatformInfo
}
