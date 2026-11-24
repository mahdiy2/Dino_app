package com.dino.message.corefeature.data.upload

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import java.io.ByteArrayOutputStream
import com.dino.message.corefeature.domain.upload.MediaFileHandle
import com.dino.message.corefeature.domain.upload.PlatformMediaMetadata
import com.dino.message.corefeature.domain.upload.PlatformMediaMetadataExtractor
import com.dino.message.corefeature.domain.upload.PlatformMediaReference

class AndroidMediaMetadataExtractor(
    private val contentResolver: ContentResolver,
) : PlatformMediaMetadataExtractor {

    constructor(context: Context) : this(context.applicationContext.contentResolver)

    fun extractWithThumbnail(uri: Uri, handle: MediaFileHandle): PlatformMediaMetadata {
        val metadata = extract(uri.toPlatformMediaReference())
        val thumbnail = runCatching {
            val bitmap = contentResolver.loadThumbnail(uri, Size(320, 320), null)
            try {
                ByteArrayOutputStream().use { output ->
                    if (bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)) {
                        output.toByteArray()
                    } else {
                        null
                    }
                }
            } finally {
                bitmap.recycle()
            }
        }.getOrNull()
        return metadata.copy(
            mimeType = metadata.mimeType ?: handle.mimeType,
            sizeBytes = handle.sizeBytes,
            thumbnailJpeg = thumbnail,
        )
    }

    override fun extract(source: PlatformMediaReference): PlatformMediaMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            contentResolver.openAssetFileDescriptor(source.toAndroidUri(), "r")?.use { descriptor ->
                if (descriptor.length >= 0L) {
                    retriever.setDataSource(
                        descriptor.fileDescriptor,
                        descriptor.startOffset,
                        descriptor.length,
                    )
                } else {
                    retriever.setDataSource(descriptor.fileDescriptor)
                }
            } ?: return PlatformMediaMetadata()
            PlatformMediaMetadata(
                durationMs = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull(),
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST),
                mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                    ?.lowercase()
            )
        } catch (_: Exception) {
            PlatformMediaMetadata()
        } finally {
            runCatching { retriever.release() }
        }
    }
}
