package com.dino.message.corefeature.data.remote.util

import com.dino.message.corefeature.data.remote.NetworkExceptionClassifier
import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.corefeature.domain.logging.AppLogger
import com.dino.message.corefeature.domain.config.AppConfig
import com.dino.message.corefeature.domain.model.asStringResourceContent
import com.dino.message.corefeature.domain.session.AuthSessionStore
import com.dino.message.corefeature.presentation.util.PopBackStackLevel
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.corefeature.presentation.util.UiEventBus
import com.dino.message.corefeature.util.RefreshTokenExpiredException
import com.dino.message.corefeature.presentation.util.UIEvent
import com.dino.message.corefeature.util.RetryableException
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.JsonConvertException
import kotlinx.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.dino.message.corefeature.presentation.navigation.AppRoute

class HttpAPIUtil constructor(
    val sPrefManager: AuthSessionStore,
    @PublishedApi
    internal val networkExceptionClassifier: NetworkExceptionClassifier,
    @PublishedApi
    internal val appLogger: AppLogger,
    private val uiEventBus: UiEventBus,
    @PublishedApi
    internal val errorMessageProvider: NetworkErrorMessageProvider = DefaultNetworkErrorMessageProvider,
    @PublishedApi
    internal val appConfig: AppConfig,
) {


    suspend inline fun <ResultType, reified ResponseType : ResponseToResultMapper<ResultType>> callAPIWithErrorHandling(
        noinline callAPI: suspend () -> HttpResponse,
        noinline callRefreshTokenAPI: suspend () -> String,
        needToUpdateToken: Boolean = true,
        useServerMessageForUnauthorized: Boolean = false,
        maxAttempts: Int = 3,
    ): Resource<ResultType> {
        var firstRetryException: Exception? = null
        var lastRetryException: Exception? = null

        val result = retry(
            times = maxAttempts.coerceAtLeast(1),
            initialDelay = 1000,
            factor = 2.0,
            onFailure = { exception ->
                if (firstRetryException == null) {
                    firstRetryException = exception
                }
                lastRetryException = exception
            }
        ) {

            try {
                val response = callAPI()
                val status = response.status
                val rawErrorBody = if (status.isSuccess()) null else readResponseBodySafely(response)

                when {
                    status.isSuccess() -> {
                        val body = response.body<ResponseType>()
                        Resource.Success(body.mapResponseToResult())
                    }

                    status == HttpStatusCode.BadRequest -> Resource.Error(
                        message = (
                            extractServerErrorMessage(rawErrorBody)
                                ?.asStringResourceContent()
                                ?: errorMessageProvider.badRequest()
                            )
                    )

                    status == HttpStatusCode.Unauthorized -> {
                        handleUnauthorized(
                            callRefreshTokenAPI = callRefreshTokenAPI,
                            needToUpdateToken = needToUpdateToken,
                            rawErrorBody = rawErrorBody,
                            useServerMessageForUnauthorized = useServerMessageForUnauthorized
                        )
                    }

                    status == HttpStatusCode.UnprocessableEntity -> {
                        val isAuthorizationHeaderError = rawErrorBody
                            .orEmpty()
                            .contains("Bad Authorization header", ignoreCase = true)

                        if (isAuthorizationHeaderError) {
                            appLogger.warning("HttpAPIUtil", "422 with bad authorization header; trying token refresh")
                            handleUnauthorized(
                                callRefreshTokenAPI = callRefreshTokenAPI,
                                needToUpdateToken = needToUpdateToken,
                                rawErrorBody = rawErrorBody,
                                useServerMessageForUnauthorized = useServerMessageForUnauthorized
                            )
                        } else {
                                Resource.Error(
                                    message = (
                                        extractServerErrorMessage(rawErrorBody)
                                            ?.asStringResourceContent()
                                            ?: errorMessageProvider.httpStatus(status.value)
                                        )
                                )
                            }
                        }

                    status == HttpStatusCode.Forbidden -> {
                        val serverError = extractServerErrorMessage(rawErrorBody)
                        Resource.Error(
                            message = serverError?.asStringResourceContent() ?: errorMessageProvider.forbidden()
                        )
                    }

                    status == HttpStatusCode.NotFound -> Resource.Error(
                        message = (
                            extractServerErrorMessage(rawErrorBody)
                                ?.asStringResourceContent()
                                ?: errorMessageProvider.notFound()
                            )
                    )

                    status == HttpStatusCode.Conflict -> Resource.Error(
                        message = (
                            extractServerErrorMessage(rawErrorBody)
                                ?.asStringResourceContent()
                                ?: errorMessageProvider.conflict()
                            )
                    )

                    status == HttpStatusCode.TooManyRequests -> Resource.Error(
                        message = (
                            extractServerErrorMessage(rawErrorBody)
                                ?.asStringResourceContent()
                                ?: errorMessageProvider.tooManyRequests()
                            )
                    )

                    status == HttpStatusCode.Gone -> Resource.Error(
                        message = (
                            extractServerErrorMessage(rawErrorBody)
                                ?.asStringResourceContent()
                                ?: errorMessageProvider.httpStatus(status.value)
                            )
                    )

                    status.value == 413 -> Resource.Error(
                        message = errorMessageProvider.payloadTooLarge()
                    )

                    status == HttpStatusCode.InternalServerError -> Resource.Error(
                        message = (
                            extractServerErrorMessage(rawErrorBody)
                                ?.asStringResourceContent()
                                ?: errorMessageProvider.internalServerError()
                            )
                    )

                    else -> Resource.Error(
                        message = (
                            extractServerErrorMessage(rawErrorBody)
                                ?.asStringResourceContent()
                                ?: errorMessageProvider.httpStatus(status.value)
                            )
                    )
                }

            } catch (e: HttpRequestTimeoutException) {
                throw RetryableException("Timeout error", e)
            } catch (e: IOException) {
                val message = e.message
                if (networkExceptionClassifier.isTimeout(e, message.orEmpty())) {
                    throw RetryableException("Timeout error", e)
                }
                throw RetryableException("Network error", e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: JsonConvertException) {
                if (appConfig.isDebug) throw e
                Resource.Error(
                    message = errorMessageProvider.convertJsonResponse()
                )
            } catch (e: RetryableException) {
                throw e
            } catch (e: Exception) {
                if (networkExceptionClassifier.isTimeout(e)) {
                    throw RetryableException("Timeout error", e)
                }
                if (networkExceptionClassifier.isNetworkFailure(e)) {
                    throw RetryableException("Network error", e)
                }
                appLogger.error("HttpAPIUtil", "Unhandled exception", e)
                Resource.Error(
                    message = e.message?.asStringResourceContent()
                        ?: errorMessageProvider.unknownError()
                )
            }
        }

        if (result != null) return result

        return Resource.Error(
            message = resolveRetryFailureMessage(firstRetryException, lastRetryException)
        )
    }

    suspend fun <ResultType> handleUnauthorized(
        callRefreshTokenAPI: suspend () -> String,
        needToUpdateToken: Boolean,
        rawErrorBody: String?,
        useServerMessageForUnauthorized: Boolean
    ): Resource<ResultType> {
        if (needToUpdateToken) {
            val newToken = try {
                callRefreshTokenAPI().trim()
            } catch (e: RefreshTokenExpiredException) {
                clearAuthSessionAndRedirectToLogin()
                return Resource.Error(
                    message = errorMessageProvider.unauthorized()
                )
            }
            if (newToken.isNotBlank()) {
                sPrefManager.setToken(newToken)
                throw RetryableException("Token refreshed, retrying request.")
            }
        }

        if (!needToUpdateToken && useServerMessageForUnauthorized) {
            extractServerErrorMessage(rawErrorBody)?.let { serverMessage ->
                return Resource.Error(
                    message = serverMessage.asStringResourceContent()
                )
            }
        }

        return Resource.Error(
            message = errorMessageProvider.unauthorized()
        )
    }

    fun extractServerErrorMessage(rawBody: String?): String? {
        val content = rawBody ?: return null
        return runCatching {
            Json.decodeFromString<JsonObject>(content)
                .get("error")
                ?.jsonPrimitive
                ?.content
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    suspend fun readResponseBodySafely(response: HttpResponse): String? {
        return runCatching { response.bodyAsText() }
            .onFailure {
                appLogger.warning("HttpAPIUtil", "Failed to consume error response body", it)
            }
            .getOrNull()
    }

    private suspend fun clearAuthSessionAndRedirectToLogin() {
        sPrefManager.clearAuthSession()
        uiEventBus.mutableUIEventFlow.emit(
            UIEvent.NavigateWithPopBackStack(
                route = AppRoute.Login,
                level = PopBackStackLevel.ALL
            )
        )
    }


    suspend fun <T> retry(
        times: Int,
        initialDelay: Long,
        factor: Double,
        onFailure: ((Exception) -> Unit)? = null,
        block: suspend () -> T
    ): T? {
        var currentDelay = initialDelay
        repeat(times - 1) {
            try {
                return block()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                onFailure?.invoke(e)
//                Log.e("Retry", "Attempt failed, retrying in $currentDelay ms", e)
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong()
        }
        return try {
            block()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            onFailure?.invoke(e)
            null
        }
    }

    @PublishedApi
    internal fun resolveRetryFailureMessage(
        firstException: Exception?,
        lastException: Exception?,
    ): com.dino.message.corefeature.domain.model.StringResourceContent {
        val selectedException = selectRetryFailureException(
            firstException = firstException,
            lastException = lastException
        )
        val retryableException = selectedException as? RetryableException
        val rootCause = retryableException?.cause ?: selectedException
        val rootMessage = rootCause?.message?.trim().orEmpty()

        return when {
            rootCause is HttpRequestTimeoutException -> {
                errorMessageProvider.requestTimedOut()
            }

            rootCause is IOException -> {
                if (networkExceptionClassifier.isTimeout(rootCause, rootMessage)) {
                    return errorMessageProvider.requestTimedOut()
                }
                errorMessageProvider.networkError()
            }

            rootCause != null && networkExceptionClassifier.isTimeout(rootCause, rootMessage) -> {
                errorMessageProvider.requestTimedOut()
            }

            rootMessage.isNotBlank() -> {
                rootMessage.asStringResourceContent()
            }

            else -> {
                errorMessageProvider.retriesExceeded()
            }
        }
    }

    @PublishedApi
    internal fun selectRetryFailureException(
        firstException: Exception?,
        lastException: Exception?,
    ): Exception? {
        fun isTokenRefreshRetryOnly(exception: Exception?): Boolean {
            if (exception !is RetryableException) return false
            if (exception.cause != null) return false
            val normalized = exception.message?.trim().orEmpty()
            return normalized.equals("Token refreshed, retrying request.", ignoreCase = true)
        }

        if (lastException != null && !isTokenRefreshRetryOnly(lastException)) {
            return lastException
        }
        if (firstException != null && !isTokenRefreshRetryOnly(firstException)) {
            return firstException
        }
        return lastException ?: firstException
    }
}
