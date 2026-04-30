package com.dino.message.chatfeature.presentation.viewmodel

import com.dino.message.chatfeature.domain.model.PrivateHistoryMessage

internal data class HistoryPendingCorrelation(
    val clientMessageId: String,
    val messageId: String,
)

internal object HistoryPendingCorrelationResolver {
    fun resolve(
        historyMessages: List<PrivateHistoryMessage>,
        currentUsername: String,
    ): List<HistoryPendingCorrelation> {
        val normalizedCurrentUsername = currentUsername.trim()
        if (normalizedCurrentUsername.isEmpty()) return emptyList()

        return historyMessages.asSequence()
            .filter { it.from.trim().equals(normalizedCurrentUsername, ignoreCase = true) }
            .mapNotNull { message ->
                val clientMessageId = message.clientMessageId?.trim().orEmpty()
                val messageId = message.messageId?.trim().orEmpty()
                if (clientMessageId.isBlank() || messageId.isBlank()) {
                    null
                } else {
                    HistoryPendingCorrelation(
                        clientMessageId = clientMessageId,
                        messageId = messageId,
                    )
                }
            }
            .distinctBy { it.clientMessageId }
            .toList()
    }
}
