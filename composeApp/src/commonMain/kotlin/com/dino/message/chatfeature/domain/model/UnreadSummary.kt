package com.dino.message.chatfeature.domain.model

data class UnreadSummary(
    val total: Int,
    val senders: List<SenderSummary>
)

data class SenderSummary(
    val sender: String,
    val count: Int,
    val lastType: String,
    val lastTimestamp: String
)
