package com.dino.message.corefeature.data.remote

import com.dino.message.corefeature.domain.logging.AppLogger
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
fun <T : HttpClientEngineConfig> createAppHttpClient(
    engineFactory: HttpClientEngineFactory<T>,
    baseUrl: String,
    isDebug: Boolean,
    appLogger: AppLogger,
): HttpClient =
    HttpClient(engineFactory) {
        defaultRequest {
            url(baseUrl)
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    appLogger.debug(tag = "Ktor", message = message)
                }
            }
            level = if (isDebug) LogLevel.ALL else LogLevel.HEADERS
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 20_000
            socketTimeoutMillis = 20_000
        }
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    explicitNulls = false
                }
            )
        }
    }
