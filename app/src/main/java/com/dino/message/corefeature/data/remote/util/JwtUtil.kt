package com.dino.message.corefeature.data.remote.util

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
