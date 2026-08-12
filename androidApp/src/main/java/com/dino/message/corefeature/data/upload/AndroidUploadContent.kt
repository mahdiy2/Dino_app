package com.dino.message.corefeature.data.upload

import android.content.ContentResolver
import android.net.Uri
import com.dino.message.corefeature.domain.upload.MediaFileHandle
import com.dino.message.corefeature.domain.upload.ReadableMediaFile
import com.dino.message.corefeature.domain.upload.UploadInputProvider
import io.ktor.utils.io.streams.asInput
import java.io.File

class AndroidUploadContentProvider(
    private val contentResolver: ContentResolver,
) : PlatformMediaResolver {
    fun canRead(uri: Uri): Boolean {
        return runCatching {
            openInputStream(uri).use { }
            true
        }.getOrDefault(false)
    }

    override fun canRead(source: PlatformMediaReference): Boolean {
        return canRead(source.toAndroidUri())
    }

    fun canRead(file: ReadableMediaFile): Boolean {
        return runCatching {
            file.openInput.open().close()
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

    fun readableMediaFile(
        uri: Uri,
        displayName: String? = null,
        mimeType: String? = null,
        sizeBytes: Long? = null,
    ): ReadableMediaFile {
        val resolvedDisplayName = displayName
            ?: MediaFileResolver.queryDisplayName(contentResolver, uri)
            ?: uri.lastPathSegment
                ?.substringAfterLast('/')
                ?.ifBlank { null }
            ?: "file"
        val resolvedMimeType = mimeType
            ?: MediaFileResolver.resolveMimeType(
                contentResolver = contentResolver,
                uri = uri,
                displayNameHint = resolvedDisplayName,
            )
        val resolvedSize = sizeBytes ?: MediaFileResolver.resolveFileSize(contentResolver, uri)
        return ReadableMediaFile(
            handle = MediaFileHandle(
                id = uri.toString(),
                displayName = resolvedDisplayName,
                mimeType = resolvedMimeType,
                sizeBytes = resolvedSize,
            ),
            openInput = UploadInputProvider {
                openInputStream(uri).asInput()
            },
        )
    }

    override fun resolvePickedMedia(
        source: PlatformMediaReference,
        displayNameHint: String?,
    ): PlatformPickedMedia {
        val uri = source.toAndroidUri()
        val displayName = displayNameHint
            ?: MediaFileResolver.queryDisplayName(contentResolver, uri)
            ?: uri.lastPathSegment
                ?.substringAfterLast('/')
                ?.ifBlank { null }
            ?: "file"
        val mimeType = MediaFileResolver.resolveMimeType(
            contentResolver = contentResolver,
            uri = uri,
            displayNameHint = displayName,
        )
        val metadata = if (mimeType.startsWith("audio/") || mimeType.startsWith("video/")) {
            metadata(source)
        } else {
            PlatformMediaMetadata()
        }
        return PlatformPickedMedia(
            reference = source,
            displayName = displayName,
            mimeType = mimeType,
            sizeBytes = MediaFileResolver.resolveFileSize(contentResolver, uri),
            durationMs = metadata.durationMs,
        )
    }

    override fun metadata(source: PlatformMediaReference): PlatformMediaMetadata {
        return AndroidMediaMetadataExtractor(contentResolver).extract(source)
    }

    override fun readableMediaFile(
        source: PlatformMediaReference,
        displayName: String?,
        mimeType: String?,
        sizeBytes: Long?,
    ): ReadableMediaFile {
        return readableMediaFile(
            uri = source.toAndroidUri(),
            displayName = displayName,
            mimeType = mimeType,
            sizeBytes = sizeBytes,
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

fun PlatformMediaReference.toAndroidUri(): Uri = Uri.parse(value)

fun Uri.toPlatformMediaReference(): PlatformMediaReference = PlatformMediaReference(toString())
