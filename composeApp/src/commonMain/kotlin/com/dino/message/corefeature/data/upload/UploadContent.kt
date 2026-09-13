package com.dino.message.corefeature.data.upload

import com.dino.message.corefeature.domain.upload.ReadableMediaFile
import com.dino.message.corefeature.domain.upload.UploadInputProvider
import io.ktor.client.request.forms.FormBuilder
import io.ktor.client.request.forms.InputProvider
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

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

fun ReadableMediaFile.canOpenForUpload(): Boolean {
    return runCatching {
        openInput.open().close()
        true
    }.getOrDefault(false)
}
