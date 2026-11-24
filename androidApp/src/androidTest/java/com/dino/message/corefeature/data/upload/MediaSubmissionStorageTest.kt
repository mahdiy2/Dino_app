package com.dino.message.corefeature.data.upload

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaSubmissionStorageTest {
    @Test
    fun pendingOperationAndServerResultSurviveDatabaseReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val path = File(context.noBackupFilesDir, "test-media-${UUID.randomUUID()}.db")
        suspend fun open(block: suspend (MediaSubmissionDatabase) -> Unit) {
            val database = Room.databaseBuilder(context, MediaSubmissionDatabase::class.java, path.absolutePath).build()
            try {
                block(database)
            } finally {
                database.close()
            }
        }
        val command = StoredMediaSubmission(
            MediaSubmissionCommand.GroupAttachment(18, """{"group_id":18,"type":"voice"}"""),
            listOf(StagedMedia("recording.m4a", "audio/mp4", 100, 1_500)),
        )
        val encoded = Json.encodeToString(command)
        try {
            open {
                it.submissions().insert(MediaSubmissionRow("id", "alice", "session", "Group chat", encoded, 100))
            }
            open {
                val dao = it.submissions()
                assertEquals(command, Json.decodeFromString<StoredMediaSubmission>(checkNotNull(dao.get("id")).command))
                assertEquals(1, dao.saveResult("id", """{"type":"voice"}"""))
            }
            open {
                val dao = it.submissions()
                assertEquals("""{"type":"voice"}""", dao.get("id")?.result)
                assertEquals(1, dao.finish("id", "complete"))
                assertEquals(0, dao.saveResult("id", "late response"))
                dao.delete("id")
                assertNull(dao.get("id"))
            }
        } finally {
            context.deleteDatabase(path.absolutePath)
        }
    }
}
