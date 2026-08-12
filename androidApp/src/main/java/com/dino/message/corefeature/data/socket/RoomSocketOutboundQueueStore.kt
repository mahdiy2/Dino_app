package com.dino.message.corefeature.data.socket

class RoomSocketOutboundQueueStore(
    private val socketOutboundQueueDao: SocketOutboundQueueDao,
) : SocketOutboundQueueStore {

    override suspend fun getAll(): List<SocketOutboundQueueRecord> {
        return socketOutboundQueueDao.getAll().map { entity ->
            SocketOutboundQueueRecord(
                queueId = entity.queueId,
                event = entity.event,
                ownerUsername = entity.ownerUsername,
                payloadType = entity.payloadType,
                payloadBody = entity.payloadBody,
                dedupeKey = entity.dedupeKey,
                attempts = entity.attempts,
                nextAttemptAtMs = entity.nextAttemptAtMs,
                createdAtMs = entity.createdAtMs,
            )
        }
    }

    override suspend fun insert(item: SocketOutboundQueueRecord): Boolean {
        return socketOutboundQueueDao.insert(
            SocketOutboundQueueEntity(
                queueId = item.queueId,
                event = item.event,
                ownerUsername = item.ownerUsername,
                payloadType = item.payloadType,
                payloadBody = item.payloadBody,
                dedupeKey = item.dedupeKey,
                attempts = item.attempts,
                nextAttemptAtMs = item.nextAttemptAtMs,
                createdAtMs = item.createdAtMs,
            )
        ) >= 0
    }

    override suspend fun deleteByQueueId(queueId: String) {
        socketOutboundQueueDao.deleteByQueueId(queueId)
    }

    override suspend fun updateRetryState(
        queueId: String,
        attempts: Int,
        nextAttemptAtMs: Long,
    ) {
        socketOutboundQueueDao.updateRetryState(
            queueId = queueId,
            attempts = attempts,
            nextAttemptAtMs = nextAttemptAtMs,
        )
    }

    override suspend fun clearAll() {
        socketOutboundQueueDao.clearAll()
    }
}
