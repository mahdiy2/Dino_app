package com.dino.message.corefeature.domain.notification

interface NotificationDedupeSnapshotStore {
    fun loadPrivateMessageSnapshot(): Map<String, Long>
    fun savePrivateMessageSnapshot(snapshot: Map<String, Long>)
}
