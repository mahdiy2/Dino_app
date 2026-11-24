package com.dino.message.corefeature.data.socket

fun interface RealtimeSocketListener {
    fun onEvent(args: Array<out Any>)
}

interface RealtimeSocketTransport {
    fun connect()
    fun disconnect()
    fun offAll()
    fun isConnected(): Boolean
    fun on(event: String, listener: RealtimeSocketListener)
    fun off(event: String, listener: RealtimeSocketListener)
    fun emit(event: String, payload: Any? = null)
}

interface RealtimeSocketTransportFactory {
    fun create(token: String): RealtimeSocketTransport?
}
