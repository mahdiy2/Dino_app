package com.dino.message.corefeature.domain.upload

import kotlin.test.assertEquals
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

        assertEquals("Voice too large. Maximum allowed is 16MB.", error)
    }

    @Test
    fun validate_rejectsUnsupportedMime() {
        val error = AttachmentUploadPolicy.validate(
            mimeType = "application/octet-stream",
            fileSizeBytes = 256L,
        )

        assertEquals("Unsupported attachment type: application/octet-stream", error)
    }
}
