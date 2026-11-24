package com.dino.message.corefeature.data.socket

internal object RealtimeSocketConnectionPlanner {
    enum class Action {
        REUSE_CONNECTED,
        WAIT_FOR_CURRENT_CONNECTION,
        CONNECT_EXISTING,
        REPLACE_TRANSPORT,
    }

    fun plan(
        currentToken: String?,
        requestedToken: String,
        hasTransport: Boolean,
        isConnected: Boolean,
        isConnecting: Boolean,
        forceReconnect: Boolean,
    ): Action {
        val canReuseTransport = hasTransport && currentToken == requestedToken && !forceReconnect
        if (!canReuseTransport) return Action.REPLACE_TRANSPORT
        if (isConnected) return Action.REUSE_CONNECTED
        if (isConnecting) return Action.WAIT_FOR_CURRENT_CONNECTION
        return Action.CONNECT_EXISTING
    }
}
