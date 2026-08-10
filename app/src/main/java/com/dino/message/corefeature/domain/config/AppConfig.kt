package com.dino.message.corefeature.domain.config

interface AppConfig {
    val baseUrl: String
    val isDebug: Boolean
    val versionName: String
    val buildType: String
    val authPasswordRequireSymbol: Boolean
}
