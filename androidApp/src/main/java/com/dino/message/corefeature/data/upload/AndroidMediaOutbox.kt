package com.dino.message.corefeature.data.upload

import com.dino.message.chatfeature.domain.model.MessageAttachment
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.domain.model.StringResourceContent
import com.dino.message.corefeature.domain.model.asStringResourceContent
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.corefeature.domain.upload.MediaFileHandle
import com.dino.message.corefeature.domain.upload.PlatformMediaMetadata
import com.dino.message.corefeature.domain.upload.ReadableMediaFile
import com.dino.message.corefeature.domain.upload.UploadInputProvider
import com.dino.message.threadfeature.domain.model.CreatePostMedia
import io.ktor.utils.io.streams.asInput
import java.io.File
import java.util.UUID
import java.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.emitAll
import kotlinx.io.readAtMostTo
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

enum class MediaWorkOutcome { Complete, Retry }

class AndroidMediaOutbox(
    private val files: File,
    private val dao: MediaSubmissionDao,
    private val session: SPrefManager,
    private val replayCapability: suspend () -> Boolean?,
    private val executor: MediaSubmissionExecutor,
    private val socket: UploadedAttachmentTransport,
    private val work: MediaSubmissionWorkScheduler,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val staging = Mutex()

    fun restore() {
        session.onMediaSubmissionSessionInvalidated = { invalidated ->
            scope.launch { staging.withLock { dao.all().filter { it.sessionId == invalidated }.forEach { purge(it) } } }
        }
        scope.launch {
            staging.withLock {
                val rows = dao.all()
                val currentSession = session.existingMediaSubmissionSessionId()
                rows.forEach {
                    when {
                        it.sessionId != currentSession || !owns(it) -> purge(it)
                        it.state == "pending" -> work.schedule(it.id, replace = false)
                        it.state != "retryable" -> deleteFiles(it.id)
                    }
                }
                val known = rows.mapTo(mutableSetOf()) { it.id }
                files.listFiles()?.filter { it.name !in known }?.forEach { it.deleteRecursively() }
            }
        }
    }

    fun <T> submit(
        command: MediaSubmissionCommand,
        media: List<CreatePostMedia>,
        serializer: KSerializer<T>,
        operationId: String = UUID.randomUUID().toString(),
        onQueued: (String) -> Unit = {},
        legacy: (() -> Flow<Resource<T>>)? = null,
    ): Flow<Resource<T>> = flow {
        if (legacy != null && withTimeoutOrNull(1_500) { replayCapability() } == false) {
            emitAll(legacy())
            return@flow
        }
        emit(Resource.Loading())
        try {
            withContext(io) {
                stage(operationId, command, media)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            emit(Resource.Error("Could not save this submission for upload".asStringResourceContent()))
            return@flow
        }
        onQueued(operationId)
        val result = dao.observe(operationId).first { it == null || it.state != "pending" }
        emit(when {
            result?.state == "complete" && result.result != null ->
                Resource.Success(Json.decodeFromString(serializer, result.result))
            result?.failure != null ->
                Resource.Error(Json.decodeFromString<StringResourceContent>(result.failure))
            else -> Resource.Error("This submission is no longer available".asStringResourceContent())
        })
    }

    fun observeCurrentSubmissions(): Flow<List<MediaSubmissionRow>> =
        dao.observeOwner(session.getUsername().orEmpty().trim()).map { rows -> rows.filter(::owns) }

    suspend fun retry(id: String) = staging.withLock {
        val row = dao.get(id) ?: return@withLock
        if (owns(row) && dao.reopenFailed(id) == 1) work.schedule(id, replace = true)
    }

    suspend fun remove(id: String) = staging.withLock {
        val row = dao.get(id) ?: return@withLock
        if (owns(row)) purge(row)
    }

    private suspend fun stage(id: String, command: MediaSubmissionCommand, media: List<CreatePostMedia>) =
        staging.withLock {
            val owner = session.getUsername()?.trim()?.takeIf { it.isNotBlank() } ?: error("Missing account")
            val sessionId = session.mediaSubmissionSessionId() ?: error("Missing session")
            val directory = directory(id)
            check(directory.mkdirs())
            var inserted = false
            try {
                val staged = media.mapIndexed { index, item ->
                    val file = File(directory, index.toString())
                    item.file.openInput.open().use { input ->
                        file.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val count = input.readAtMostTo(buffer)
                                if (count <= 0) break
                                output.write(buffer, 0, count)
                            }
                            output.fd.sync()
                        }
                    }
                    stagedMetadata(item, file.length())
                }
                val row = MediaSubmissionRow(
                    id = id,
                    owner = owner,
                    sessionId = sessionId,
                    destination = destination(command),
                    command = Json.encodeToString(StoredMediaSubmission(command, staged)),
                    createdAt = System.currentTimeMillis(),
                )
                check(owns(row))
                currentCoroutineContext().ensureActive()
                withContext(NonCancellable) {
                    dao.insert(row)
                    inserted = true
                    work.schedule(id, replace = false)
                }
            } finally {
                if (!inserted) directory.deleteRecursively()
            }
        }

    suspend fun process(id: String, attempt: Int): MediaWorkOutcome {
        val row = dao.get(id) ?: return MediaWorkOutcome.Complete
        if (!owns(row)) {
            purge(row)
            return MediaWorkOutcome.Complete
        }
        if (row.state != "pending") return MediaWorkOutcome.Complete
        return try {
            processPending(row, attempt)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: MediaReplayAccountChanged) {
            purge(row)
            MediaWorkOutcome.Complete
        } catch (_: Exception) {
            retryOrFail(row, attempt, "Could not complete the pending submission".asStringResourceContent())
        }
    }

    private suspend fun processPending(row: MediaSubmissionRow, attempt: Int): MediaWorkOutcome {
        val stored = Json.decodeFromString<StoredMediaSubmission>(row.command)
        val request = MediaReplayRequest(
            row.id,
            authorization = { tokenFor(row) },
            tokenBelongsToAccount = { token -> tokenOwner(token) == row.owner },
        )
        val result = row.result ?: run {
            if (tokenFor(row) == null) throw MediaReplayAccountChanged()
            when (replayCapability()) {
                null -> return retryOrFail(row, attempt, "Upload service is temporarily unavailable".asStringResourceContent())
                false -> return fail(row, "This server does not support resumable media submissions".asStringResourceContent())
                true -> Unit
            }

            val upload = withContext(request) { executor.submit(stored.command, reopen(row.id, stored.files)) }
            when (upload) {
                is Resource.Error -> {
                    val code = request.lastStatus
                    return if (code == null || code == 401 || code == 429 || code >= 500) {
                        retryOrFail(row, attempt, upload.message)
                    } else {
                        fail(row, upload.message)
                    }
                }
                is Resource.Loading -> return MediaWorkOutcome.Retry
                is Resource.Success -> {
                    if (!owns(row)) throw MediaReplayAccountChanged()
                    if (dao.saveResult(row.id, upload.data) == 0) return MediaWorkOutcome.Complete
                    upload.data
                }
            }
        }

        currentCoroutineContext().ensureActive()
        if (!owns(row)) throw MediaReplayAccountChanged()
        if (dao.get(row.id)?.state != "pending") return MediaWorkOutcome.Complete
        val delivery = when (val command = stored.command) {
            is MediaSubmissionCommand.PrivateAttachment ->
                Triple("send_message", "private:${command.recipient}:${row.id}", command.preparedMessage)
            is MediaSubmissionCommand.GroupAttachment ->
                Triple("send_group_message", "group:${command.groupId}:${row.id}", command.preparedMessage)
            else -> null
        }
        if (delivery != null) {
            val attachment = Json.decodeFromString<MessageAttachment>(result)
            val prepared = Json.parseToJsonElement(delivery.third).jsonObject
            val attachmentFields = buildJsonObject {
                put("type", attachment.type)
                attachment.url?.let { put("url", it) }
                attachment.mimeType?.let { put("mime_type", it) }
                attachment.fileName?.let { put("file_name", it) }
                attachment.sizeBytes?.let { put("size_bytes", it) }
            }
            val payload = JsonObject(prepared + mapOf(
                "type" to (prepared["type"] ?: JsonPrimitive(attachment.type)),
                "client_message_id" to JsonPrimitive(row.id),
                "attachment" to attachmentFields,
            )).toString()
            if (!socket.enqueue(row.owner, row.sessionId, delivery.first, payload, delivery.second)) {
                return retryOrFail(row, attempt, "Could not queue the uploaded message".asStringResourceContent())
            }
        }
        if (dao.finish(row.id, "complete") == 1) {
            if (stored.command is MediaSubmissionCommand.Post && owns(row)) session.markThreadRefreshAfterPost()
            deleteFiles(row.id)
        }
        return MediaWorkOutcome.Complete
    }

    private fun reopen(id: String, entries: List<StagedMedia>): List<CreatePostMedia> =
        entries.mapIndexed { index, entry ->
            val path = File(directory(id), index.toString())
            CreatePostMedia(
                file = ReadableMediaFile(
                    MediaFileHandle(path.toURI().toString(), entry.displayName, entry.mimeType, entry.sizeBytes),
                    UploadInputProvider { path.inputStream().asInput() },
                    PlatformMediaMetadata(durationMs = entry.durationMs),
                ),
                trackTitle = entry.trackTitle,
                trackArtist = entry.trackArtist,
            )
        }

    private fun destination(command: MediaSubmissionCommand): String = when (command) {
        is MediaSubmissionCommand.PrivateAttachment -> "Private chat"
        is MediaSubmissionCommand.GroupAttachment -> "Group chat"
        is MediaSubmissionCommand.Profile -> "Profile"
        is MediaSubmissionCommand.Post -> if (command.music) "Music post" else "Post"
        is MediaSubmissionCommand.Story -> "Story"
    }

    private fun owns(row: MediaSubmissionRow): Boolean = session.getUserLogIn() &&
        session.getUsername()?.trim() == row.owner &&
        session.existingMediaSubmissionSessionId() == row.sessionId

    private fun tokenFor(row: MediaSubmissionRow): String? =
        if (owns(row)) session.getToken().takeIf { tokenOwner(it) == row.owner && owns(row) } else null

    private fun tokenOwner(token: String): String? = runCatching {
        val payload = token.trim().split('.')[1]
        val json = Base64.getUrlDecoder().decode(payload).decodeToString()
        Json.parseToJsonElement(json).jsonObject["sub"]?.jsonPrimitive?.content
    }.getOrNull()

    private suspend fun retryOrFail(row: MediaSubmissionRow, attempt: Int, reason: StringResourceContent) =
        if (attempt < 7) MediaWorkOutcome.Retry else {
            dao.finish(row.id, "retryable", Json.encodeToString(reason))
            MediaWorkOutcome.Complete
        }

    private suspend fun fail(row: MediaSubmissionRow, reason: StringResourceContent): MediaWorkOutcome {
        if (dao.finish(row.id, "failed", Json.encodeToString(reason)) == 1) deleteFiles(row.id)
        return MediaWorkOutcome.Complete
    }

    private suspend fun purge(row: MediaSubmissionRow) {
        work.cancel(row.id)
        dao.delete(row.id)
        deleteFiles(row.id)
    }

    private fun directory(id: String): File = File(files, UUID.fromString(id).toString())

    private fun deleteFiles(id: String) { directory(id).deleteRecursively() }
}
