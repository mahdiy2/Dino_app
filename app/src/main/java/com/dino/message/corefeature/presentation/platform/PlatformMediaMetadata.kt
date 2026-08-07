package com.dino.message.corefeature.presentation.platform

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

data class PlatformMediaMetadata(
    val durationMs: Long? = null,
    val title: String? = null,
    val artist: String? = null,
    val mimeType: String? = null,
)

data class PlatformMediaReference(
    val value: String,
)

interface PlatformMediaMetadataExtractor {
    fun extract(source: PlatformMediaReference): PlatformMediaMetadata
}

class AndroidMediaMetadataExtractor(
    context: Context
) : PlatformMediaMetadataExtractor {
    private val appContext = context.applicationContext

    fun extract(uri: Uri): PlatformMediaMetadata {
        return extract(PlatformMediaReference(uri.toString()))
    }

    override fun extract(source: PlatformMediaReference): PlatformMediaMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            val uri = Uri.parse(source.value)
            retriever.setDataSource(appContext, uri)
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
