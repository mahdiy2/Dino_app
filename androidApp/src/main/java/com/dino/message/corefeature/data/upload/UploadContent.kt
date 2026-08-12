package com.dino.message.corefeature.data.upload

import io.ktor.client.request.forms.FormBuilder
import io.ktor.client.request.forms.InputProvider
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.core.Input

fun interface UploadInputProvider {
    fun open(): Input
}

data class MediaFileHandle(
    val id: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long? = null,
)

data class PlatformMediaReference(
    val value: String,
)

data class PlatformMediaMetadata(
    val durationMs: Long? = null,
    val title: String? = null,
    val artist: String? = null,
    val mimeType: String? = null,
)

data class PlatformPickedMedia(
    val reference: PlatformMediaReference,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long? = null,
    val durationMs: Long? = null,
)

interface PlatformMediaMetadataExtractor {
    fun extract(source: PlatformMediaReference): PlatformMediaMetadata
}

interface PlatformMediaResolver {
    fun canRead(source: PlatformMediaReference): Boolean
    fun resolvePickedMedia(
        source: PlatformMediaReference,
        displayNameHint: String? = null,
    ): PlatformPickedMedia
    fun metadata(source: PlatformMediaReference): PlatformMediaMetadata
    fun readableMediaFile(
        source: PlatformMediaReference,
        displayName: String? = null,
        mimeType: String? = null,
        sizeBytes: Long? = null,
    ): ReadableMediaFile
}

data class ReadableMediaFile(
    val handle: MediaFileHandle,
    val openInput: UploadInputProvider,
)

data class UploadFilePart(
    val formName: String,
    val fileName: String,
    val mimeType: String,
    val size: Long?,
    val openInput: UploadInputProvider,
)

fun FormBuilder.appendUploadFile(part: UploadFilePart) {
    append(
        key = part.formName,
        value = InputProvider(size = part.size) {
            part.openInput.open()
        },
        headers = Headers.build {
            append(HttpHeaders.ContentType, part.mimeType)
            append(
                HttpHeaders.ContentDisposition,
                "form-data; name=\"${part.formName}\"; filename=\"${part.fileName}\""
            )
        },
    )
}

fun ReadableMediaFile.toUploadFilePart(formName: String): UploadFilePart {
    return UploadFilePart(
        formName = formName,
        fileName = handle.displayName,
        mimeType = handle.mimeType,
        size = handle.sizeBytes,
        openInput = openInput,
    )
}
