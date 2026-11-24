package com.dino.message.corefeature.data.upload

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import com.dino.message.corefeature.data.remote.util.appendTokenToHeader
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/** Binds all attempts of one persisted submission to its owner and operation ID. */
class MediaReplayRequest(
    val operationId: String,
    private val authorization: () -> String?,
    private val tokenBelongsToAccount: (String) -> Boolean,
) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<MediaReplayRequest>

    var lastStatus: Int? = null
        private set

    fun recordStatus(status: Int?) {
        lastStatus = status
    }

    fun requireCurrentAccount(): String =
        authorization()?.takeIf(tokenBelongsToAccount) ?: throw MediaReplayAccountChanged()

    fun requireAccountToken(token: String) {
        requireCurrentAccount()
        if (!tokenBelongsToAccount(token)) throw MediaReplayAccountChanged()
    }

    fun authorize(builder: HttpRequestBuilder) {
        val token = requireCurrentAccount()
        builder.appendTokenToHeader(token)
        builder.header("Idempotency-Key", operationId)
        builder.header("X-Dino-Media-Replay", "1")
    }
}

class MediaReplayAccountChanged : IllegalStateException("The upload belongs to another session")

suspend fun currentMediaReplayRequest(): MediaReplayRequest? = coroutineContext[MediaReplayRequest]
