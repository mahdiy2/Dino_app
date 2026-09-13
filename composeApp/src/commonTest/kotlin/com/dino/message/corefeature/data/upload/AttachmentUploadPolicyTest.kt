package com.dino.message.corefeature.domain.upload

import com.dino.message.corefeature.domain.model.AppStringKey
import com.dino.message.corefeature.domain.model.StringResourceContent
import com.dino.message.corefeature.domain.model.toFormatArgs
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.Test

class AttachmentUploadPolicyTest {

    @Test
    fun normalizeMimeType_stripsParametersAndLowercases() {
        val mimeType = AttachmentUploadPolicy.normalizeMimeType("Image/WebP; charset=binary")

        assertEquals("image/webp", mimeType)
    }

    @Test
    fun validate_acceptsKnownMimeAtBoundarySize() {
        val error = AttachmentUploadPolicy.validate(
            mimeType = "image/webp",
            fileSizeBytes = AttachmentUploadPolicy.MAX_IMAGE_SIZE_BYTES,
        )

        assertNull(error)
    }

    @Test
    fun validate_rejectsKnownMimeOverBoundarySize() {
        val error = AttachmentUploadPolicy.validate(
            mimeType = "audio/webm",
            fileSizeBytes = AttachmentUploadPolicy.MAX_AUDIO_SIZE_BYTES + 1,
        )

        val content = assertIs<StringResourceContent.Key>(error)
        assertEquals(AppStringKey.VoiceAttachmentTooLarge, content.key)
        assertEquals(listOf("16MB"), content.args.toFormatArgs().toList())
    }

    @Test
    fun validate_rejectsUnsupportedMime() {
        val error = AttachmentUploadPolicy.validate(
            mimeType = "application/octet-stream",
            fileSizeBytes = 256L,
        )

        val content = assertIs<StringResourceContent.Key>(error)
        assertEquals(AppStringKey.UnsupportedAttachmentType, content.key)
        assertEquals(listOf("application/octet-stream"), content.args.toFormatArgs().toList())
    }
}
