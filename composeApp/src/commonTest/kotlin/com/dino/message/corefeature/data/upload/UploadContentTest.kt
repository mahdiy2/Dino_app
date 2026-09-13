package com.dino.message.corefeature.data.upload

import com.dino.message.corefeature.domain.upload.MediaFileHandle
import com.dino.message.corefeature.domain.upload.ReadableMediaFile
import com.dino.message.corefeature.domain.upload.UploadInputProvider
import io.ktor.utils.io.core.ByteReadPacket
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
}
