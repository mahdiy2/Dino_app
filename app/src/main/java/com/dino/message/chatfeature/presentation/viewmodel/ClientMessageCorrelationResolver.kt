package com.dino.message.chatfeature.presentation.viewmodel

import java.util.ArrayDeque

internal data class CorrelatedPendingItem<T>(
    val clientMessageId: String,
    val item: T,
)

internal object ClientMessageCorrelationResolver {
    fun <T> consumePending(
        pendingByClientId: MutableMap<String, T>,
        pendingOrder: ArrayDeque<String>,
        clientMessageId: String?,
    ): CorrelatedPendingItem<T>? {
        val normalizedClientMessageId = clientMessageId
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        if (normalizedClientMessageId != null) {
            val pending = pendingByClientId.remove(normalizedClientMessageId) ?: return null
            pendingOrder.remove(normalizedClientMessageId)
            return CorrelatedPendingItem(
                clientMessageId = normalizedClientMessageId,
                item = pending,
            )
        }

        while (pendingOrder.isNotEmpty()) {
            val fallbackClientMessageId = pendingOrder.removeFirst()
            val fallbackPending = pendingByClientId.remove(fallbackClientMessageId)
            if (fallbackPending != null) {
                return CorrelatedPendingItem(
                    clientMessageId = fallbackClientMessageId,
                    item = fallbackPending,
                )
            }
        }
        return null
    }
}
