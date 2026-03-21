package com.dino.message.corefeature.data.remote.util

import android.content.Context
import android.util.Log
import com.dino.message.BuildConfig
import com.dino.message.R
import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.domain.model.asStringResourceContent
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.util.RetryableException
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.JsonConvertException
import io.ktor.utils.io.errors.IOException
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
        needToUpdateToken: Boolean = true
    ): Resource<ResultType> {

        return retry(times = 3, initialDelay = 1000, factor = 2.0) {

            try {
                val response = callAPI()
                when {
                    response.status.isSuccess() -> {
                        val body = response.body<ResponseType>()
                        Resource.Success(body.mapResponseToResult())
                    }

                    response.status == HttpStatusCode.BadRequest -> Resource.Error(
                        message = R.string.error_bad_request.asStringResourceContent()
                    )

                    response.status == HttpStatusCode.Unauthorized -> {
                        handleUnauthorized(callRefreshTokenAPI = callRefreshTokenAPI, needToUpdateToken = needToUpdateToken)
                    }

                    response.status == HttpStatusCode.UnprocessableEntity -> {
                        val rawBody = runCatching { response.bodyAsText() }.getOrDefault("")
                        val isAuthorizationHeaderError = rawBody.contains("Bad Authorization header", ignoreCase = true)

                        if (isAuthorizationHeaderError) {
                            Log.w("HttpAPIUtil", "422 with bad authorization header; trying token refresh")
                            handleUnauthorized(callRefreshTokenAPI = callRefreshTokenAPI, needToUpdateToken = needToUpdateToken)
                        } else {
                            Resource.Error(
                                message = context.getString(
                                    R.string.error_with_http_status_code,
                                    response.status.value
                                ).asStringResourceContent()
                            )
                        }
                    }

                    response.status == HttpStatusCode.Forbidden -> {
                        val rawBody = runCatching { response.bodyAsText() }.getOrDefault("")
                        val serverError = runCatching {
                            Json.decodeFromString<JsonObject>(rawBody)
                                .get("error")
                                ?.jsonPrimitive?.content
                        }.getOrNull()
                        Resource.Error(
                            message = (serverError ?: context.getString(R.string.error_forbidden))
                                .asStringResourceContent()
                        )
                    }

                    response.status == HttpStatusCode.NotFound -> Resource.Error(
                        message = R.string.error_not_found.asStringResourceContent()
                    )

                    response.status == HttpStatusCode.Conflict -> Resource.Error(
                        message = R.string.error_conflict.asStringResourceContent()
                    )

                    response.status.value == 413 -> Resource.Error(
                        message = R.string.error_payload_too_large.asStringResourceContent()
                    )

                    response.status == HttpStatusCode.InternalServerError -> Resource.Error(
                        message = R.string.internal_server_error.asStringResourceContent()
                    )

                    else -> Resource.Error(
                        message = context.getString(
                            R.string.error_with_http_status_code,
                            response.status.value
                        ).asStringResourceContent()
                    )
                }

            } catch (e: UnresolvedAddressException) {
                throw RetryableException("Network error", e)
            } catch (e: IOException) {
                throw RetryableException("Network error", e)
            } catch (e: HttpRequestTimeoutException) {
                throw RetryableException("Timeout error", e)
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
        } ?: Resource.Error(
            message = R.string.error_retries_exceeded.asStringResourceContent()
        )
    }

    suspend fun <ResultType> handleUnauthorized(
        callRefreshTokenAPI: suspend () -> String,
        needToUpdateToken: Boolean
    ): Resource<ResultType> {
        if (needToUpdateToken) {
            val newToken = callRefreshTokenAPI().trim()
            if (newToken.isNotBlank()) {
                sPrefManager.setToken(newToken)
                throw RetryableException("Token refreshed, retrying request.")
            }
        }

        return Resource.Error(
            message = R.string.error_unauthorized.asStringResourceContent()
        )
    }


    suspend fun <T> retry(
        times: Int,
        initialDelay: Long,
        factor: Double,
        block: suspend () -> T
    ): T? {
        var currentDelay = initialDelay
        repeat(times - 1) {
            try {
                return block()
            } catch (e: Exception) {
//                Log.e("Retry", "Attempt failed, retrying in $currentDelay ms", e)
            }
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong()
        }
        return try {
            block()
        } catch (e: Exception) {
            null
        }
    }
}
