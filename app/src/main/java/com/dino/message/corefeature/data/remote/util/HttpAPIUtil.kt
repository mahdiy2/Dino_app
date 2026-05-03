package com.dino.message.corefeature.data.remote.util

import android.content.Context
import android.util.Log
import com.dino.message.BuildConfig
import com.dino.message.DinoOrderApplication
import com.dino.message.R
import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.domain.model.asStringResourceContent
import com.dino.message.corefeature.presentation.util.PopBackStackLevel
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.util.RefreshTokenExpiredException
import com.dino.message.corefeature.presentation.util.UIEvent
import com.dino.message.corefeature.util.RetryableException
import com.ramcosta.composedestinations.generated.destinations.LoginScreenDestination
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.JsonConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.channels.UnresolvedAddressException
import javax.inject.Inject

class HttpAPIUtil @Inject constructor(
    val sPrefManager: SPrefManager,
    val context: Context
) {


    suspend inline fun <ResultType, reified ResponseType : ResponseToResultMapper<ResultType>> callAPIWithErrorHandling(
        noinline callAPI: suspend () -> HttpResponse,
        noinline callRefreshTokenAPI: suspend () -> String,
        needToUpdateToken: Boolean = true,
        useServerMessageForUnauthorized: Boolean = false
    ): Resource<ResultType> {
        var firstRetryException: Exception? = null

        val result = retry(
            times = 3,
            initialDelay = 1000,
            factor = 2.0,
            onFailure = { exception ->
                if (firstRetryException == null) {
                    firstRetryException = exception
                }
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
                                ?: context.getString(R.string.error_bad_request)
                            ).asStringResourceContent()
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
                            Log.w("HttpAPIUtil", "422 with bad authorization header; trying token refresh")
                            handleUnauthorized(
                                callRefreshTokenAPI = callRefreshTokenAPI,
                                needToUpdateToken = needToUpdateToken,
                                rawErrorBody = rawErrorBody,
                                useServerMessageForUnauthorized = useServerMessageForUnauthorized
                            )
                        } else {
                            Resource.Error(
                                message = context.getString(
                                    R.string.error_with_http_status_code,
                                    status.value
                                ).asStringResourceContent()
                            )
                        }
                    }

                    status == HttpStatusCode.Forbidden -> {
                        val serverError = extractServerErrorMessage(rawErrorBody)
                        Resource.Error(
                            message = (serverError ?: context.getString(R.string.error_forbidden))
                                .asStringResourceContent()
                        )
                    }

                    status == HttpStatusCode.NotFound -> Resource.Error(
                        message = (
                            extractServerErrorMessage(rawErrorBody)
                                ?: context.getString(R.string.error_not_found)
                            ).asStringResourceContent()
                    )

                    status == HttpStatusCode.Conflict -> Resource.Error(
                        message = (
                            extractServerErrorMessage(rawErrorBody)
                                ?: context.getString(R.string.error_conflict)
                            ).asStringResourceContent()
                    )

                    status == HttpStatusCode.TooManyRequests -> Resource.Error(
                        message = (
                            extractServerErrorMessage(rawErrorBody)
                                ?: context.getString(R.string.error_too_many_login_attempts)
                            ).asStringResourceContent()
                    )

                    status == HttpStatusCode.Gone -> Resource.Error(
                        message = (
                            extractServerErrorMessage(rawErrorBody)
                                ?: context.getString(
                                    R.string.error_with_http_status_code,
                                    status.value
                                )
                            ).asStringResourceContent()
                    )

                    status.value == 413 -> Resource.Error(
                        message = R.string.error_payload_too_large.asStringResourceContent()
                    )

                    status == HttpStatusCode.InternalServerError -> Resource.Error(
                        message = R.string.internal_server_error.asStringResourceContent()
                    )

                    else -> Resource.Error(
                        message = context.getString(
                            R.string.error_with_http_status_code,
                            status.value
                        ).asStringResourceContent()
                    )
                }

            } catch (e: UnresolvedAddressException) {
                throw RetryableException("Network error", e)
            } catch (e: IOException) {
                throw RetryableException("Network error", e)
            } catch (e: HttpRequestTimeoutException) {
                throw RetryableException("Timeout error", e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: JsonConvertException) {
                if (BuildConfig.DEBUG) throw e
                Resource.Error(
                    message = R.string.error_convert_json_response.asStringResourceContent()
                )
            } catch (e: RetryableException) {
                throw e
            } catch (e: Exception) {
                Log.e("HttpAPIUtil", "Unhandled exception", e)
                Resource.Error(
                    message = e.message?.asStringResourceContent()
                        ?: R.string.unknown_error_occurred.asStringResourceContent()
                )
            }
        }

        if (result != null) return result

        return Resource.Error(
            message = resolveRetryFailureMessage(firstRetryException)
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
                    message = R.string.error_unauthorized.asStringResourceContent()
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
            message = R.string.error_unauthorized.asStringResourceContent()
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
                Log.w("HttpAPIUtil", "Failed to consume error response body", it)
            }
            .getOrNull()
    }

    private suspend fun clearAuthSessionAndRedirectToLogin() {
        sPrefManager.clearAuthSession()
        val app = context.applicationContext as? DinoOrderApplication ?: return
        app.mutableUIEventFlow.emit(
            UIEvent.NavigateWithPopBackStack(
                direction = LoginScreenDestination,
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
    internal fun resolveRetryFailureMessage(exception: Exception?): com.dino.message.corefeature.domain.model.StringResourceContent {
        val retryableException = exception as? RetryableException
        val rootCause = retryableException?.cause ?: exception
        val rootMessage = rootCause?.message?.trim().orEmpty()

        return when {
            rootCause is HttpRequestTimeoutException -> {
                R.string.error_request_timed_out.asStringResourceContent()
            }

            rootCause is UnresolvedAddressException -> {
                R.string.network_error.asStringResourceContent()
            }

            rootCause is IOException -> {
                R.string.network_error.asStringResourceContent()
            }

            rootMessage.isNotBlank() -> {
                rootMessage.asStringResourceContent()
            }

            else -> {
                R.string.error_retries_exceeded.asStringResourceContent()
            }
        }
    }
}
