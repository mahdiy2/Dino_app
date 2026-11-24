package com.dino.message.corefeature.data.socket

import org.json.JSONArray
import org.json.JSONObject

internal object AndroidSocketOutboundPayloadCodec {
    fun encode(payload: Any?): SocketOutboundPayloadCodec.PersistedPayload =
        SocketOutboundPayloadCodec.encode(
            when (payload) {
                null -> SocketOutboundPayload.Null
                is JSONObject -> SocketOutboundPayload.JsonObject(payload.toString())
                is JSONArray -> SocketOutboundPayload.JsonArray(payload.toString())
                is Boolean -> SocketOutboundPayload.BooleanValue(payload)
                is Number -> SocketOutboundPayload.NumberValue(payload.toString())
                else -> SocketOutboundPayload.Text(payload.toString())
            }
        )

    fun decode(payloadType: String, payloadBody: String?): Any? =
        when (val payload = SocketOutboundPayloadCodec.decode(payloadType, payloadBody)) {
            SocketOutboundPayload.Null -> null
            is SocketOutboundPayload.JsonObject -> payloadBody?.let { JSONObject(payload.value) }
            is SocketOutboundPayload.JsonArray -> payloadBody?.let { JSONArray(payload.value) }
            is SocketOutboundPayload.BooleanValue -> payload.value
            is SocketOutboundPayload.NumberValue -> payload.value.toDoubleOrNull() ?: 0.0
            is SocketOutboundPayload.Text -> if (payloadType == "string") payload.value else payloadBody
        }
}
