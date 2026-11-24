package com.dino.message.corefeature.data.socket

internal sealed interface SocketOutboundPayload {
    data object Null : SocketOutboundPayload
    data class JsonObject(val value: String) : SocketOutboundPayload
    data class JsonArray(val value: String) : SocketOutboundPayload
    data class Text(val value: String) : SocketOutboundPayload
    data class BooleanValue(val value: Boolean) : SocketOutboundPayload
    data class NumberValue(val value: String) : SocketOutboundPayload
}

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

    fun encode(payload: SocketOutboundPayload): PersistedPayload = when (payload) {
        SocketOutboundPayload.Null -> PersistedPayload(TYPE_NULL, null)
        is SocketOutboundPayload.JsonObject -> PersistedPayload(TYPE_JSON_OBJECT, payload.value)
        is SocketOutboundPayload.JsonArray -> PersistedPayload(TYPE_JSON_ARRAY, payload.value)
        is SocketOutboundPayload.Text -> PersistedPayload(TYPE_STRING, payload.value)
        is SocketOutboundPayload.BooleanValue -> PersistedPayload(TYPE_BOOLEAN, payload.value.toString())
        is SocketOutboundPayload.NumberValue -> PersistedPayload(TYPE_NUMBER, payload.value)
    }

    fun decode(payloadType: String, payloadBody: String?): SocketOutboundPayload = when (payloadType) {
        TYPE_NULL -> SocketOutboundPayload.Null
        TYPE_JSON_OBJECT -> SocketOutboundPayload.JsonObject(payloadBody.orEmpty())
        TYPE_JSON_ARRAY -> SocketOutboundPayload.JsonArray(payloadBody.orEmpty())
        TYPE_STRING -> SocketOutboundPayload.Text(payloadBody.orEmpty())
        TYPE_BOOLEAN -> SocketOutboundPayload.BooleanValue(payloadBody?.toBooleanStrictOrNull() ?: false)
        TYPE_NUMBER -> SocketOutboundPayload.NumberValue(payloadBody ?: "0.0")
        else -> SocketOutboundPayload.Text(payloadBody.orEmpty())
    }
}
