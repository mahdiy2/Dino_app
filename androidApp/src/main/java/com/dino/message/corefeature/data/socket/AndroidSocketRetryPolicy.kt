package com.dino.message.corefeature.data.socket

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.json.JSONObject

internal object AndroidSocketRetryPolicy {
    fun requiresRetry(event: String): Boolean = SocketRetryPolicy.requiresRetry(event)

    fun autoDedupeKey(event: String, payload: Any?): String? {
        if (!requiresRetry(event) || payload !is JSONObject) return null
        return SocketRetryPolicy.autoDedupeKey(event, Json.parseToJsonElement(payload.toString()).jsonObject)
    }
}
