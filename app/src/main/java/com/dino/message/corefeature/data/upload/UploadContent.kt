package com.dino.message.corefeature.data.upload

import io.ktor.client.request.forms.FormBuilder
import io.ktor.client.request.forms.InputProvider
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.core.Input

fun interface UploadInputProvider {
    fun open(): Input
}

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
