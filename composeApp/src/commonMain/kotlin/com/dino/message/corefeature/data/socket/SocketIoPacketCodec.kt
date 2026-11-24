package com.dino.message.corefeature.data.socket

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

internal object SocketIoPacketCodec {
    private const val ENGINE_OPEN = '0'
    private const val ENGINE_CLOSE = '1'
    private const val ENGINE_PING = '2'
    private const val ENGINE_PONG = '3'
    private const val SOCKET_CONNECT = "40"
    private const val SOCKET_DISCONNECT = "41"
    private const val SOCKET_EVENT = "42"
    private const val SOCKET_CONNECT_ERROR = "44"

    sealed interface IncomingPacket {
        data object EngineOpen : IncomingPacket
        data object EngineClose : IncomingPacket
        data class Ping(val payload: String) : IncomingPacket
        data object Connected : IncomingPacket
        data object Disconnected : IncomingPacket
        data class ConnectError(val payload: Any?) : IncomingPacket
        data class Event(val name: String, val arguments: List<Any?>) : IncomingPacket
        data object Ignore : IncomingPacket
    }

    private val json = Json { ignoreUnknownKeys = true }

    fun connectPacket(token: String): String = SOCKET_CONNECT + buildJsonObject {
        put("token", token)
    }

    fun pongPacket(pingPayload: String): String = ENGINE_PONG + pingPayload

    fun eventPacket(event: String, payload: Any?): String = SOCKET_EVENT + buildJsonArray {
        add(JsonPrimitive(event))
        if (payload != null) add(payload.toJsonElement())
    }

    fun decode(text: String): IncomingPacket {
        if (text.isEmpty()) return IncomingPacket.Ignore
        return when {
            text.first() == ENGINE_OPEN -> IncomingPacket.EngineOpen
            text.first() == ENGINE_CLOSE -> IncomingPacket.EngineClose
            text.first() == ENGINE_PING -> IncomingPacket.Ping(text.drop(1))
            text.startsWith(SOCKET_CONNECT_ERROR) -> IncomingPacket.ConnectError(
                text.drop(SOCKET_CONNECT_ERROR.length).decodeJsonValue()
            )
            text.startsWith(SOCKET_EVENT) -> decodeEvent(text.drop(SOCKET_EVENT.length))
            text.startsWith(SOCKET_DISCONNECT) -> IncomingPacket.Disconnected
            text.startsWith(SOCKET_CONNECT) -> IncomingPacket.Connected
            else -> IncomingPacket.Ignore
        }
    }

    private fun decodeEvent(body: String): IncomingPacket {
        val values = runCatching { json.parseToJsonElement(body).jsonArray }.getOrNull()
            ?: return IncomingPacket.Ignore
        val event = values.firstOrNull()?.let { first ->
            runCatching { first.jsonPrimitive.content }.getOrNull()
        } ?: return IncomingPacket.Ignore
        return IncomingPacket.Event(
            name = event,
            arguments = values.drop(1).map(JsonElement::toTransportValue),
        )
    }

    private fun String.decodeJsonValue(): Any? = runCatching {
        json.parseToJsonElement(this).toTransportValue()
    }.getOrElse { this }

    private fun Any?.toJsonElement(): JsonElement = when (this) {
        null -> JsonNull
        is JsonElement -> this
        is String -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Byte -> JsonPrimitive(this)
        is Short -> JsonPrimitive(this)
        is Int -> JsonPrimitive(this)
        is Long -> JsonPrimitive(this)
        is Float -> JsonPrimitive(this)
        is Double -> JsonPrimitive(this)
        else -> JsonPrimitive(toString())
    }

    private fun JsonElement.toTransportValue(): Any? = when (this) {
        JsonNull -> null
        is JsonArray -> this
        is kotlinx.serialization.json.JsonObject -> this
        is JsonPrimitive -> when {
            isString -> content
            booleanOrNull != null -> booleanOrNull
            longOrNull != null -> longOrNull
            doubleOrNull != null -> doubleOrNull
            else -> content
        }
    }
}
