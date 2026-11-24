package com.dino.message.corefeature.data.socket

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

sealed interface SocketOutboundPayload {
    data object Null : SocketOutboundPayload
    data class JsonObject(val value: String) : SocketOutboundPayload
    data class JsonArray(val value: String) : SocketOutboundPayload
    data class Text(val value: String) : SocketOutboundPayload
    data class BooleanValue(val value: Boolean) : SocketOutboundPayload
    data class NumberValue(val value: String) : SocketOutboundPayload
}

object SocketOutboundPayloadCodec {
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

internal fun SocketOutboundPayloadCodec.encodeValue(payload: Any?): SocketOutboundPayloadCodec.PersistedPayload =
    encode(when (payload) {
        null, JsonNull -> SocketOutboundPayload.Null
        is JsonObject -> SocketOutboundPayload.JsonObject(payload.toString())
        is JsonArray -> SocketOutboundPayload.JsonArray(payload.toString())
        is JsonPrimitive -> if (payload.isString) {
            SocketOutboundPayload.Text(payload.content)
        } else encodePrimitive(payload.content)
        is Boolean -> SocketOutboundPayload.BooleanValue(payload)
        is Number -> SocketOutboundPayload.NumberValue(payload.toString())
        else -> SocketOutboundPayload.Text(payload.toString())
    })

private fun encodePrimitive(value: String): SocketOutboundPayload =
    value.toBooleanStrictOrNull()?.let { SocketOutboundPayload.BooleanValue(it) }
        ?: SocketOutboundPayload.NumberValue(value)

internal fun SocketOutboundPayloadCodec.decodeValue(type: String, body: String?): Any? =
    when (val payload = decode(type, body)) {
        SocketOutboundPayload.Null -> null
        is SocketOutboundPayload.JsonObject -> Json.parseToJsonElement(payload.value)
            .let { it as JsonObject }
        is SocketOutboundPayload.JsonArray -> Json.parseToJsonElement(payload.value)
            .let { it as JsonArray }
        is SocketOutboundPayload.Text -> payload.value
        is SocketOutboundPayload.BooleanValue -> payload.value
        is SocketOutboundPayload.NumberValue -> payload.value.toLongOrNull() ?: payload.value.toDoubleOrNull() ?: 0.0
    }
