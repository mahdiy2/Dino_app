package com.dino.message.authfeature.domain.util

object RegisterConfirmErrorPolicy {
    fun shouldClearPendingSession(errorMessage: String): Boolean {
        val normalized = errorMessage.lowercase()
        return normalized.contains("expired") ||
            normalized.contains("not found") ||
            normalized.contains("already exists")
    }
}
