package com.dino.message.chatfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UnreadResponse(
    val total: Int,
    val senders: List<SenderSummary>
) : ResponseToResultMapper<UnreadResponse> {
    override fun mapResponseToResult(): UnreadResponse = this
}

@Serializable
data class SenderSummary(
    val sender: String,
    val count: Int,
    @SerialName("last_type") val lastType: String,
    @SerialName("last_timestamp") val lastTimestamp: String
)
