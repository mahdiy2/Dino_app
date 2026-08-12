package com.dino.message.corefeature.data.socket

import org.json.JSONArray
import org.json.JSONObject

internal object SocketOutboundPayloadCodec {

    private const val TYPE_NULL = "null"
    private const val TYPE_JSON_OBJECT = "json_object"
    private const val TYPE_JSON_ARRAY = "json_array"
    private const val TYPE_STRING = "string"
    private const val TYPE_BOOLEAN = "boolean"
    private const val TYPE_NUMBER = "number"

    data class PersistedPayload(
        val payloadType: String,
        val payloadBody: String?,
    )

    fun encode(payload: Any?): PersistedPayload {
        return when (payload) {
            null -> PersistedPayload(payloadType = TYPE_NULL, payloadBody = null)
            is JSONObject -> PersistedPayload(payloadType = TYPE_JSON_OBJECT, payloadBody = payload.toString())
            is JSONArray -> PersistedPayload(payloadType = TYPE_JSON_ARRAY, payloadBody = payload.toString())
            is String -> PersistedPayload(payloadType = TYPE_STRING, payloadBody = payload)
            is Boolean -> PersistedPayload(payloadType = TYPE_BOOLEAN, payloadBody = payload.toString())
            is Number -> PersistedPayload(payloadType = TYPE_NUMBER, payloadBody = payload.toString())
            else -> PersistedPayload(payloadType = TYPE_STRING, payloadBody = payload.toString())
        }
    }

    fun decode(payloadType: String, payloadBody: String?): Any? {
        return when (payloadType) {
            TYPE_NULL -> null
            TYPE_JSON_OBJECT -> payloadBody?.let { JSONObject(it) }
            TYPE_JSON_ARRAY -> payloadBody?.let { JSONArray(it) }
            TYPE_STRING -> payloadBody.orEmpty()
            TYPE_BOOLEAN -> payloadBody?.toBooleanStrictOrNull() ?: false
            TYPE_NUMBER -> payloadBody?.toDoubleOrNull() ?: 0.0
            else -> payloadBody
        }
    }
}
