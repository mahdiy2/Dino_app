package com.dino.message.corefeature.data.socket

import kotlinx.coroutines.flow.StateFlow

interface RealtimeSocketGateway {
    val queuedOutboundCount: StateFlow<Int>

    fun connect(forceReconnect: Boolean = false): Boolean
    suspend fun awaitConnected(timeoutMs: Long = 7_000L): Boolean
    fun disconnect(clearListeners: Boolean = false)
    fun isConnected(): Boolean
    fun registerListener(
        owner: String,
        event: String,
        listener: (Array<out Any>) -> Unit,
    )
    fun unregisterOwner(owner: String)
    fun emit(event: String, payload: Any? = null): Boolean
    fun emitNonRetryable(event: String, payload: Any? = null, reason: String): Boolean
    fun emitWithRetry(
        event: String,
        payload: Any? = null,
        dedupeKey: String? = null,
    ): AppSocketManager.EmitWithRetryResult
}
