package com.dino.message.corefeature.data.config

import com.dino.message.BuildConfig
import com.dino.message.corefeature.domain.config.AppConfig

object AndroidAppConfig : AppConfig {
    override val baseUrl: String = BuildConfig.BASE_URL
    override val isDebug: Boolean = BuildConfig.DEBUG
    override val versionName: String = BuildConfig.VERSION_NAME
    override val buildType: String = BuildConfig.BUILD_TYPE
    override val authPasswordRequireSymbol: Boolean = BuildConfig.AUTH_PASSWORD_REQUIRE_SYMBOL
}
