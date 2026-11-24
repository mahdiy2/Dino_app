package com.dino.message.corefeature.data.remote.util

import com.dino.message.corefeature.domain.session.AuthSessionStore
import com.dino.message.corefeature.data.upload.MediaReplayAccountChanged
import com.dino.message.corefeature.data.upload.currentMediaReplayRequest
import kotlinx.coroutines.CancellationException
import com.dino.message.corefeature.domain.crypto.Base64Codec
import com.dino.message.corefeature.domain.logging.AppLogger
import com.dino.message.corefeature.domain.system.AppClock
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpHeaders

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
    sPrefManager: AuthSessionStore,
    refreshAccessToken: suspend () -> String,
    logTag: String,
    appClock: AppClock,
    appLogger: AppLogger,
    thresholdSeconds: Long = 90L,
    base64Codec: Base64Codec,
) {
    val submission = currentMediaReplayRequest()
    val accessToken = (submission?.requireCurrentAccount() ?: sPrefManager.getToken())
        .trim()
        .removePrefix("Bearer ")
        .removePrefix("bearer ")
        .trim()
    if (accessToken.isBlank()) return

    val expirationEpochSeconds = decodeJwtExpirationEpochSeconds(accessToken, base64Codec) ?: return
    val currentEpochSeconds = appClock.currentTimeMillis() / 1000L
    val remainingSeconds = expirationEpochSeconds - currentEpochSeconds
    if (remainingSeconds > thresholdSeconds) return

    try {
        val refreshedToken = refreshAccessToken()
            .trim()
            .removePrefix("Bearer ")
            .removePrefix("bearer ")
            .trim()
        if (refreshedToken.isNotBlank()) {
            submission?.requireCurrentAccount()
            sPrefManager.setToken(refreshedToken)
        }
    } catch (changed: MediaReplayAccountChanged) {
        throw changed
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (exception: Exception) {
        appLogger.warning(logTag, "Failed to pre-refresh token before upload", exception)
    }
}

internal fun decodeJwtExpirationEpochSeconds(
    accessToken: String,
    base64Codec: Base64Codec,
): Long? {
    val segments = accessToken.split(".")
    if (segments.size < 2) return null
    val payloadSegment = segments[1]

    val normalizedPayload = buildString {
        append(payloadSegment)
        when (length % 4) {
            2 -> append("==")
            3 -> append("=")
            1 -> return null
        }
    }

    val payloadJson = runCatching {
        val decodedBytes = base64Codec.decodeUrlSafe(normalizedPayload, noWrap = true)
        decodedBytes.decodeToString()
    }.getOrNull() ?: return null

    val expRegex = Regex("\"exp\"\\s*:\\s*(\\d+)")
    return expRegex.find(payloadJson)
        ?.groupValues
        ?.getOrNull(1)
        ?.toLongOrNull()
}
