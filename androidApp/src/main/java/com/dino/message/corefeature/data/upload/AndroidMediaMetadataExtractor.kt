package com.dino.message.corefeature.data.upload

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever

class AndroidMediaMetadataExtractor(
    private val contentResolver: ContentResolver,
) : PlatformMediaMetadataExtractor {

    constructor(context: Context) : this(context.applicationContext.contentResolver)

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
