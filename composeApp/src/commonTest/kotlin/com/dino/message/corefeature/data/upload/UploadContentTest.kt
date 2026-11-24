package com.dino.message.corefeature.data.upload

import com.dino.message.corefeature.domain.upload.MediaFileHandle
import com.dino.message.corefeature.domain.upload.ReadableMediaFile
import com.dino.message.corefeature.domain.upload.UploadInputProvider
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.test.runTest
import kotlinx.io.readString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UploadContentTest {
    @Test
    fun toUploadFilePartPreservesFormNameAndFileMetadata() {
        val file = ReadableMediaFile(
            handle = MediaFileHandle(
                id = "content://media/1",
                displayName = "photo.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 42L,
            ),
            openInput = UploadInputProvider { ByteReadPacket(byteArrayOf(1, 2, 3)) },
        )

        val part = file.toUploadFilePart(formName = "media[]")

        assertEquals("media[]", part.formName)
        assertEquals("photo.jpg", part.fileName)
        assertEquals("image/jpeg", part.mimeType)
        assertEquals(42L, part.size)
        assertTrue(file.canOpenForUpload())
    }

    @Test
    fun multipartBodyCanReopenFilesForRetry() = runTest {
        var opens = 0
        val file = ReadableMediaFile(
            handle = MediaFileHandle("picked-file", "track.mp3", "audio/mpeg", 4L),
            openInput = UploadInputProvider {
                opens++
                ByteReadPacket("data".encodeToByteArray())
            },
        )
        val body = MultiPartFormDataContent(formData {
            append("track_title", "Track")
            appendUploadFile(file.toUploadFilePart(formName = "media[]"))
        }, boundary = "upload-test")

        suspend fun writeBody(): String {
            val channel = ByteChannel(autoFlush = true)
            body.writeTo(channel)
            channel.flushAndClose()
            return channel.readRemaining().readString()
        }

        val firstBody = writeBody()
        assertTrue(firstBody.contains("name=\"media[]\"") || firstBody.contains("name=media[]"))
        assertTrue(firstBody.contains("filename=\"track.mp3\""))
        assertTrue(firstBody.contains("Content-Type: audio/mpeg"))
        assertTrue(firstBody.contains("track_title"))
        assertTrue(firstBody.contains("\r\ndata\r\n"))
        assertEquals(firstBody, writeBody())
        assertEquals(2, opens)
    }
}
