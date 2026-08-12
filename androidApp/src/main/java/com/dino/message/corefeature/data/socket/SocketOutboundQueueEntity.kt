package com.dino.message.corefeature.data.socket

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "socket_outbound_queue",
    indices = [
        Index(name = "index_socket_outbound_queue_nextAttemptAtMs", value = ["nextAttemptAtMs"]),
        Index(name = "index_socket_outbound_queue_dedupeKey", value = ["dedupeKey"], unique = true),
    ],
)
data class SocketOutboundQueueEntity(
    @PrimaryKey val queueId: String,
    val event: String,
    val ownerUsername: String,
    val payloadType: String,
    val payloadBody: String?,
    val dedupeKey: String?,
    val attempts: Int,
    val nextAttemptAtMs: Long,
    val createdAtMs: Long,
)
