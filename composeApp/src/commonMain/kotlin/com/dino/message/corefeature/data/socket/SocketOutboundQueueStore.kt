package com.dino.message.corefeature.data.socket

import kotlinx.serialization.Serializable

@Serializable
data class SocketOutboundQueueRecord(
    val queueId: String,
    val event: String,
    val ownerUsername: String,
    val payloadType: String,
    val payloadBody: String?,
    val dedupeKey: String?,
    val attempts: Int,
    val nextAttemptAtMs: Long,
    val createdAtMs: Long,
)

interface SocketOutboundQueueStore {
    suspend fun getAll(): List<SocketOutboundQueueRecord>
    suspend fun insert(item: SocketOutboundQueueRecord): Boolean
    suspend fun deleteByQueueId(queueId: String)
    suspend fun updateRetryState(queueId: String, attempts: Int, nextAttemptAtMs: Long)
    suspend fun clearAll()
}
