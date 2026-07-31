package com.dino.message.corefeature.data.upload

import android.content.ContentResolver
import android.net.Uri
import io.ktor.utils.io.streams.asInput
import java.io.File

class AndroidUploadContentProvider(
    private val contentResolver: ContentResolver,
) {
    fun canRead(uri: Uri): Boolean {
        return runCatching {
            openInputStream(uri).use { }
            true
        }.getOrDefault(false)
    }

    fun filePart(
        uri: Uri,
        formName: String,
        fileName: String,
        mimeType: String,
        size: Long?,
    ): UploadFilePart {
        return UploadFilePart(
            formName = formName,
            fileName = fileName,
            mimeType = mimeType,
            size = size,
            openInput = UploadInputProvider {
                openInputStream(uri).asInput()
            },
        )
    }

    private fun openInputStream(uri: Uri): java.io.InputStream {
        if (uri.scheme == "file") {
            val path = uri.path ?: error("File path is missing")
            val file = File(path)
            if (!file.exists()) error("File does not exist")
            return file.inputStream()
        }
        return contentResolver.openInputStream(uri)
            ?: error("Unable to open input stream")
    }
}
