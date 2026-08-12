package com.dino.message.corefeature.data.upload

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File

object MediaFileResolver {
    private const val OCTET_STREAM = "application/octet-stream"

    private val fallbackMimeTypeByExtension = mapOf(
        "heic" to "image/heic",
        "heif" to "image/heif",
        "avif" to "image/avif",
        "mov" to "video/quicktime",
        "m4v" to "video/mp4",
        "m4a" to "audio/mp4",
        "aac" to "audio/aac",
        "3gp" to "video/3gpp",
        "3gpp" to "video/3gpp",
        "oga" to "audio/ogg",
        "ogv" to "video/ogg",
    )

    fun resolveMimeType(contentResolver: ContentResolver, uri: Uri, displayNameHint: String? = null): String {
        val resolverMimeType = normalizeMimeType(contentResolver.getType(uri))
        if (!resolverMimeType.isNullOrBlank() && resolverMimeType != OCTET_STREAM) {
            return resolverMimeType
        }

        val extensionMimeType = sequenceOf(
            displayNameHint,
            queryDisplayName(contentResolver, uri),
            uri.lastPathSegment,
        )
            .mapNotNull { candidate -> mimeTypeFromName(candidate) }
            .firstOrNull()

        return when {
            !extensionMimeType.isNullOrBlank() -> extensionMimeType
            !resolverMimeType.isNullOrBlank() -> resolverMimeType
            else -> OCTET_STREAM
        }
    }

    fun queryDisplayName(contentResolver: ContentResolver, uri: Uri): String? {
        val cursor = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?: return null
        return cursor.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex == -1 || !it.moveToFirst()) return@use null
            it.getString(nameIndex)
        }
    }

    fun resolveFileSize(contentResolver: ContentResolver, uri: Uri): Long? {
        if (uri.scheme == "file") {
            val path = uri.path ?: return null
            val file = File(path)
            return if (file.exists()) file.length() else null
        }

        val sizeFromColumns = queryOpenableSize(contentResolver, uri)
        if (sizeFromColumns != null && sizeFromColumns >= 0L) {
            return sizeFromColumns
        }

        val sizeFromAssetFd = runCatching {
            contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                descriptor.length.takeIf { it >= 0L }
            }
        }.getOrNull()
        if (sizeFromAssetFd != null) {
            return sizeFromAssetFd
        }

        return runCatching {
            contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                descriptor.statSize.takeIf { it >= 0L }
            }
        }.getOrNull()
    }

    private fun mimeTypeFromName(name: String?): String? {
        val extension = name
            ?.substringAfterLast('.', "")
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?.trim()
            ?.lowercase()
            ?: fallbackMimeTypeByExtension[extension]
    }

    private fun normalizeMimeType(rawMimeType: String?): String? {
        return rawMimeType
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }
    }

    private fun queryOpenableSize(contentResolver: ContentResolver, uri: Uri): Long? {
        val cursor = contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?: return null
        return cursor.use {
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex == -1 || !it.moveToFirst()) return@use null
            if (it.isNull(sizeIndex)) return@use null
            it.getLong(sizeIndex)
        }
    }
}
