package com.dino.message.corefeature.data.socket

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Queue records are scoped to the supplied application database and browser origin. */
class IndexedDbSocketOutboundQueueStore(
    private val databaseName: String = "dino-realtime",
) : SocketOutboundQueueStore {
    override suspend fun getAll(): List<SocketOutboundQueueRecord> =
        Json.decodeFromString<List<SocketOutboundQueueRecord>>(execute("read"))
            .sortedWith(compareBy({ it.nextAttemptAtMs }, { it.createdAtMs }))

    override suspend fun insert(item: SocketOutboundQueueRecord): Boolean =
        execute("insert", Json.encodeToString(item)) == "true"

    override suspend fun deleteByQueueId(queueId: String) {
        execute("delete", queueId)
    }

    override suspend fun updateRetryState(queueId: String, attempts: Int, nextAttemptAtMs: Long) {
        execute("update", queueId, attempts, nextAttemptAtMs.toDouble())
    }

    override suspend fun clearAll() {
        execute("clear")
    }

    private suspend fun execute(
        operation: String, argument: String = "", attempts: Int = 0, nextAttempt: Double = 0.0,
    ): String = suspendCancellableCoroutine { continuation ->
        transact(databaseName, operation, argument, attempts, nextAttempt,
            { continuation.resume(it) }, { continuation.resumeWithException(IllegalStateException(it)) })
    }
}

@JsFun("""
(name, operation, argument, attempts, nextAttempt, resolve, reject) => {
    const fail = error => reject(String(error));
    const open = indexedDB.open(name, 1);
    open.onupgradeneeded = () => {
        const queue = open.result.createObjectStore('queue', { keyPath: 'queueId' });
        queue.createIndex('dedupe', 'dedupeKey', { unique: true });
    };
    open.onerror = () => fail(open.error);
    open.onsuccess = () => {
        const db = open.result;
        let result = '';
        const tx = db.transaction('queue', operation === 'read' ? 'readonly' : 'readwrite');
        const queue = tx.objectStore('queue');
        tx.oncomplete = () => { db.close(); resolve(result); };
        tx.onabort = () => { db.close(); fail(tx.error); };
        if (operation === 'read') {
            const request = queue.getAll();
            request.onsuccess = () => { result = JSON.stringify(request.result); };
        } else if (operation === 'insert') {
            const request = queue.add(JSON.parse(argument));
            request.onsuccess = () => { result = 'true'; };
            request.onerror = event => {
                if (request.error.name === 'ConstraintError') {
                    event.preventDefault();
                    result = 'false';
                }
            };
        } else if (operation === 'delete') {
            queue.delete(argument);
        } else if (operation === 'update') {
            const request = queue.get(argument);
            request.onsuccess = () => {
                const row = request.result;
                if (row) queue.put({ ...row, attempts, nextAttemptAtMs: nextAttempt });
            };
        } else {
            queue.clear();
        }
    };
}
""")
private external fun transact(
    name: String, operation: String, argument: String, attempts: Int, nextAttempt: Double,
    resolve: (String) -> Unit, reject: (String) -> Unit,
)
