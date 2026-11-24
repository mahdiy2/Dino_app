package com.dino.message.corefeature.data.upload

import com.dino.message.chatfeature.domain.model.MessageAttachment
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.data.spref.SecureKeyValueStore
import com.dino.message.corefeature.domain.model.asStringResourceContent
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.corefeature.domain.upload.MediaFileHandle
import com.dino.message.corefeature.domain.upload.ReadableMediaFile
import com.dino.message.corefeature.domain.upload.UploadInputProvider
import com.dino.message.threadfeature.domain.model.CreatePostMedia
import com.dino.message.threadfeature.domain.model.StoryUploadResult
import java.io.File
import java.util.Base64
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.readString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AndroidMediaOutboxTest {
    @get:Rule val folder = TemporaryFolder()

    @Test
    fun offlineSubmissionSurvivesObserverAndOutboxRestartWithStableOperationId() = runTest {
        val db = MemoryDao()
        val session = authenticatedSession()
        val work = TestWork()
        val attempts = mutableListOf<String>()
        var online = false
        val story = StoryUploadResult(25, "https://example.test/story", "image")
        val submit = MediaSubmissionExecutor { command, files ->
            assertEquals(MediaSubmissionCommand.Story(listOf("bob")), command)
            assertEquals("content", files.single().file.openInput.open().use { it.readString() })
            val replay = checkNotNull(currentMediaReplayRequest())
            attempts += replay.operationId
            if (attempts.size == 1) Resource.Error("Lost response".asStringResourceContent())
            else Resource.Success(Json.encodeToString(story))
        }

        val first = outbox(db, session, work, this, submit) { if (online) true else null }
        val observer = launch {
            first.submit(
                MediaSubmissionCommand.Story(listOf("bob")), listOf(source()), StoryUploadResult.serializer(),
                operationId = OPERATION,
            ).collect {}
        }
        advanceUntilIdle()
        observer.cancelAndJoin()
        assertEquals(listOf(OPERATION), work.scheduled)
        assertEquals(MediaWorkOutcome.Retry, first.process(OPERATION, attempt = 0))
        assertTrue(attempts.isEmpty())

        online = true
        val restarted = outbox(db, session, work, this, submit) { true }
        restarted.restore()
        advanceUntilIdle()
        assertEquals(listOf(OPERATION, OPERATION), work.scheduled)
        assertEquals(MediaWorkOutcome.Retry, restarted.process(OPERATION, attempt = 1))
        assertEquals(MediaWorkOutcome.Complete, restarted.process(OPERATION, attempt = 2))
        assertEquals(listOf(OPERATION, OPERATION), attempts)
        assertEquals("complete", db.get(OPERATION)?.state)
        assertEquals(story, Json.decodeFromString<StoryUploadResult>(checkNotNull(db.get(OPERATION)?.result)))
        assertFalse(File(folder.root, OPERATION).exists())
    }

    @Test
    fun attachmentReceiptSurvivesHandoffFailureAndLogoutCancelsPendingWork() = runTest {
        val db = MemoryDao()
        val session = authenticatedSession()
        val work = TestWork()
        var uploads = 0
        val deliveries = mutableListOf<String>()
        val transport = UploadedAttachmentTransport { owner, sessionId, event, payload, key ->
            assertEquals(session.existingMediaSubmissionSessionId(), sessionId)
            assertEquals("alice", owner)
            assertEquals("send_message", event)
            assertEquals("private:bob:$OPERATION", key)
            deliveries += payload
            deliveries.size == 2
        }
        val outbox = outbox(db, session, work, this, MediaSubmissionExecutor { _, _ ->
            uploads++
            Resource.Success(Json.encodeToString(MessageAttachment("image", "https://example.test/photo")))
        }, transport) { true }
        outbox.restore()
        val observer = launch {
            outbox.submit(
                MediaSubmissionCommand.PrivateAttachment("bob", """{"to":"bob","encrypted_key":"opaque"}"""),
                listOf(source()), MessageAttachment.serializer(), operationId = OPERATION,
            ).collect {}
        }
        advanceUntilIdle()
        assertEquals(MediaWorkOutcome.Retry, outbox.process(OPERATION, attempt = 0))
        assertEquals(MediaWorkOutcome.Complete, outbox.process(OPERATION, attempt = 1))
        observer.join()
        assertEquals(1, uploads)
        assertEquals(deliveries[0], deliveries[1])
        assertTrue(deliveries.first().contains(OPERATION))

        val pending = launch {
            outbox.submit(
                MediaSubmissionCommand.Story(emptyList()), listOf(source()), StoryUploadResult.serializer(),
                operationId = SECOND_OPERATION,
            ).collect {}
        }
        advanceUntilIdle()
        session.clearAuthSession()
        advanceUntilIdle()
        pending.join()
        assertTrue(work.cancelled.contains(SECOND_OPERATION))
        assertNull(db.get(SECOND_OPERATION))
        assertFalse(File(folder.root, SECOND_OPERATION).exists())
        assertEquals(MediaWorkOutcome.Complete, outbox.process(SECOND_OPERATION, 0))
        assertEquals(1, uploads)
    }

    @Test
    fun olderServerUsesForegroundSubmissionWithoutStaging() = runTest {
        val db = MemoryDao()
        val work = TestWork()
        val outbox = outbox(db, authenticatedSession(), work, this, MediaSubmissionExecutor { _, _ ->
            error("Old server must use the original path")
        }) { false }
        val story = StoryUploadResult(45, "https://example.test/story", "image")
        val result = outbox.submit(
            MediaSubmissionCommand.Story(emptyList()), listOf(source()), StoryUploadResult.serializer(),
            operationId = OPERATION,
            legacy = { flowOf(Resource.Success(story)) },
        ).toList().single()
        assertEquals(story, (result as Resource.Success).data)
        assertTrue(db.all().isEmpty())
        assertTrue(work.scheduled.isEmpty())
        assertFalse(File(folder.root, OPERATION).exists())
    }

    private fun outbox(
        db: MediaSubmissionDao,
        session: SPrefManager,
        work: TestWork,
        scope: TestScope,
        executor: MediaSubmissionExecutor,
        transport: UploadedAttachmentTransport = UploadedAttachmentTransport { _, _, _, _, _ -> error("Unexpected send") },
        capability: suspend () -> Boolean?,
    ) = AndroidMediaOutbox(
        folder.root, db, session, capability, executor, transport, work,
        io = StandardTestDispatcher(scope.testScheduler), scope = scope,
    )

    private fun source() = CreatePostMedia(ReadableMediaFile(
        MediaFileHandle("temporary", "photo.jpg", "image/jpeg"),
        UploadInputProvider { Buffer().apply { write("content".encodeToByteArray()) } },
    ))

    private fun authenticatedSession() = SPrefManager(MemoryStore()).apply {
        setUsername("alice")
        setToken("header.${Base64.getUrlEncoder().withoutPadding().encodeToString("""{"sub":"alice"}""".encodeToByteArray())}.signature")
        setUserLogIn(true)
    }

    private class TestWork : MediaSubmissionWorkScheduler {
        val scheduled = mutableListOf<String>()
        val cancelled = mutableListOf<String>()
        override fun schedule(id: String, replace: Boolean) { scheduled += id }
        override fun cancel(id: String) { cancelled += id }
    }

    private class MemoryStore : SecureKeyValueStore {
        private val data = mutableMapOf<String, Any>()
        override fun putString(key: String, value: String) { data[key] = value }
        override fun getString(key: String, defaultValue: String?) = data[key] as? String ?: defaultValue
        override fun putBoolean(key: String, value: Boolean) { data[key] = value }
        override fun getBoolean(key: String, defaultValue: Boolean) = data[key] as? Boolean ?: defaultValue
        override fun putLong(key: String, value: Long) { data[key] = value }
        override fun getLong(key: String, defaultValue: Long) = data[key] as? Long ?: defaultValue
        override fun remove(key: String) = apply { data.remove(key) }
        override fun clear() = data.clear()
        override fun apply() = Unit
    }

    private class MemoryDao : MediaSubmissionDao {
        private val rows = MutableStateFlow<Map<String, MediaSubmissionRow>>(emptyMap())
        override suspend fun insert(row: MediaSubmissionRow) { rows.value += row.id to row }
        override suspend fun get(id: String) = rows.value[id]
        override fun observe(id: String) = rows.map { it[id] }
        override fun observeOwner(owner: String) = rows.map { it.values.filter { row -> row.owner == owner } }
        override suspend fun all() = rows.value.values.toList()
        override suspend fun saveResult(id: String, result: String) =
            update(id, "pending") { it.copy(result = result) }
        override suspend fun finish(id: String, state: String, failure: String?) =
            update(id, "pending") { it.copy(state = state, failure = failure) }
        override suspend fun reopenFailed(id: String) =
            update(id, "retryable") { it.copy(state = "pending", failure = null) }
        override suspend fun delete(id: String) { rows.value -= id }

        private fun update(id: String, state: String, transform: (MediaSubmissionRow) -> MediaSubmissionRow): Int {
            val row = rows.value[id]?.takeIf { it.state == state } ?: return 0
            rows.value += id to transform(row)
            return 1
        }
    }

    private companion object {
        const val OPERATION = "2f651d60-874b-4306-8e8a-e6f806a622d0"
        const val SECOND_OPERATION = "fbae5715-a278-4baa-8453-493a35e8b81d"
    }
}
