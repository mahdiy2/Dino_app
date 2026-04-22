package com.dino.message.chatfeature.presentation.viewmodel

internal data class ChatSocketConnectPlan(
    val shouldRegisterListeners: Boolean,
    val shouldConnectSocket: Boolean,
    val shouldRunConnectedSync: Boolean,
)

internal object ChatSocketConnectPlanner {
    fun plan(
        forceReconnect: Boolean,
        isConnected: Boolean,
        isSocketConnecting: Boolean,
    ): ChatSocketConnectPlan {
        if (!forceReconnect && isConnected) {
            return ChatSocketConnectPlan(
                shouldRegisterListeners = true,
                shouldConnectSocket = false,
                shouldRunConnectedSync = true,
            )
        }

        if (!forceReconnect && isSocketConnecting) {
            return ChatSocketConnectPlan(
                shouldRegisterListeners = true,
                shouldConnectSocket = false,
                shouldRunConnectedSync = false,
            )
        }

        return ChatSocketConnectPlan(
            shouldRegisterListeners = true,
            shouldConnectSocket = true,
            shouldRunConnectedSync = false,
        )
    }
}
