package com.dino.message.corefeature.data.remote.util

import android.util.Base64
import android.util.Log
import com.dino.message.corefeature.data.spref.SPrefManager
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpHeaders
import java.nio.charset.StandardCharsets

/**
 * This function adds the provided token to the "Authorization" header of an HTTP request.
 * The token is normalized to avoid malformed headers.
 *
 * @param token The token to be added to the header.
 */
fun HttpRequestBuilder.appendTokenToHeader(token: String?) {
    val normalizedToken = token
        ?.trim()
        ?.removePrefix("Bearer ")
        ?.removePrefix("bearer ")
        ?.trim()
        .orEmpty()

    if (normalizedToken.isBlank()) return

    headers.remove(HttpHeaders.Authorization)
    headers.append(HttpHeaders.Authorization, "Bearer $normalizedToken")
}

suspend fun ensureFreshAccessTokenIfNearExpiry(
    sPrefManager: SPrefManager,
    refreshAccessToken: suspend () -> String,
    logTag: String,
    thresholdSeconds: Long = 90L
) {
    val accessToken = sPrefManager.getToken()
        .trim()
        .removePrefix("Bearer ")
        .removePrefix("bearer ")
        .trim()
    if (accessToken.isBlank()) return

    val expirationEpochSeconds = decodeJwtExpirationEpochSeconds(accessToken) ?: return
    val currentEpochSeconds = System.currentTimeMillis() / 1000L
    val remainingSeconds = expirationEpochSeconds - currentEpochSeconds
    if (remainingSeconds > thresholdSeconds) return

    try {
        val refreshedToken = refreshAccessToken()
            .trim()
            .removePrefix("Bearer ")
            .removePrefix("bearer ")
            .trim()
        if (refreshedToken.isNotBlank()) {
            sPrefManager.setToken(refreshedToken)
        }
    } catch (exception: Exception) {
        Log.w(logTag, "Failed to pre-refresh token before upload", exception)
    }
}

private fun decodeJwtExpirationEpochSeconds(accessToken: String): Long? {
    val segments = accessToken.split(".")
    if (segments.size < 2) return null
    val payloadSegment = segments[1]

    val normalizedPayload = buildString {
        append(payloadSegment.replace('-', '+').replace('_', '/'))
        when (length % 4) {
            2 -> append("==")
            3 -> append("=")
            1 -> return null
        }
    }

    val payloadJson = runCatching {
        val decodedBytes = Base64.decode(normalizedPayload, Base64.DEFAULT)
        String(decodedBytes, StandardCharsets.UTF_8)
    }.getOrNull() ?: return null

    val expRegex = Regex("\"exp\"\\s*:\\s*(\\d+)")
    return expRegex.find(payloadJson)
        ?.groupValues
        ?.getOrNull(1)
        ?.toLongOrNull()
}
