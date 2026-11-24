package com.dino.message.corefeature.data.upload

import com.dino.message.corefeature.domain.upload.MediaFileHandle
import com.dino.message.corefeature.domain.upload.PlatformMediaMetadata
import com.dino.message.corefeature.domain.upload.ReadableMediaFile
import com.dino.message.corefeature.domain.upload.UploadInputProvider
import com.dino.message.corefeature.domain.upload.UploadMediaCategory
import com.dino.message.corefeature.domain.upload.UploadMediaPicker
import com.dino.message.corefeature.domain.upload.resolveMediaFileMimeType
import io.ktor.utils.io.streams.asInput
import java.awt.Component
import java.awt.EventQueue
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import javax.sound.sampled.AudioSystem
import javax.swing.JFileChooser
import javax.swing.filechooser.FileFilter
import kotlin.concurrent.thread
import kotlin.math.roundToInt

class DesktopUploadMediaPicker(private val parent: Component? = null) : UploadMediaPicker {
    override fun pick(
        categories: Set<UploadMediaCategory>,
        onResult: (Result<ReadableMediaFile?>) -> Unit,
    ) {
        EventQueue.invokeLater {
            val selection = runCatching {
                val chooser = JFileChooser().apply {
                    isAcceptAllFileFilterUsed = false
                    fileFilter = object : FileFilter() {
                        override fun getDescription() = categories.joinToString { it.name }
                        override fun accept(file: File): Boolean =
                            file.isDirectory || categories.any { mimeType(file).startsWith("${it.mimePrefix}/") }
                    }
                }
                if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                    chooser.selectedFile
                } else {
                    null
                }
            }
            selection.fold(
                onSuccess = { file ->
                    if (file == null) {
                        onResult(Result.success(null))
                    } else {
                        thread(isDaemon = true, name = "read-media-metadata") {
                            val result = runCatching { readMedia(file) }
                            EventQueue.invokeLater { onResult(result) }
                        }
                    }
                },
                onFailure = { onResult(Result.failure(it)) },
            )
        }
    }

    private fun mimeType(file: File): String =
        resolveMediaFileMimeType(file.name, runCatching { Files.probeContentType(file.toPath()) }.getOrNull())

    private fun readMedia(file: File): ReadableMediaFile {
        val path = file.toPath()
        val mime = mimeType(file)
        val size = Files.size(path)
        val audioFormat = if (mime.startsWith("audio/")) {
            runCatching { AudioSystem.getAudioFileFormat(file) }.getOrNull()
        } else {
            null
        }
        val duration = audioFormat?.let {
            (it.getProperty("duration") as? Number)?.toLong()?.div(1_000)
                ?: it.format.frameRate.takeIf { rate -> rate > 0 && it.frameLength >= 0 }
                    ?.let { rate -> (it.frameLength * 1_000.0 / rate).toLong() }
        }
        return ReadableMediaFile(
            handle = MediaFileHandle(file.toURI().toString(), file.name, mime, size),
            openInput = UploadInputProvider { file.inputStream().asInput() },
            metadata = PlatformMediaMetadata(
                durationMs = duration,
                title = audioFormat?.getProperty("title") as? String,
                artist = audioFormat?.getProperty("author") as? String,
                mimeType = mime,
                sizeBytes = size,
                thumbnailJpeg = if (mime.startsWith("image/")) imageThumbnail(file) else null,
            ),
        )
    }

    private fun imageThumbnail(file: File): ByteArray? = runCatching {
        ImageIO.createImageInputStream(file).use { input ->
            val reader = ImageIO.getImageReaders(input).asSequence().firstOrNull() ?: return null
            try {
                reader.input = input
                val step = (maxOf(reader.getWidth(0), reader.getHeight(0)) / 320).coerceAtLeast(1)
                val image = reader.read(0, reader.defaultReadParam.apply {
                    setSourceSubsampling(step, step, 0, 0)
                })
                val scale = minOf(1.0, 320.0 / maxOf(image.width, image.height))
                val thumbnail = BufferedImage(
                    (image.width * scale).roundToInt().coerceAtLeast(1),
                    (image.height * scale).roundToInt().coerceAtLeast(1),
                    BufferedImage.TYPE_INT_RGB,
                )
                val graphics = thumbnail.createGraphics()
                try {
                    graphics.drawImage(image, 0, 0, thumbnail.width, thumbnail.height, null)
                } finally {
                    graphics.dispose()
                }
                ByteArrayOutputStream().use {
                    if (ImageIO.write(thumbnail, "jpeg", it)) it.toByteArray() else null
                }
            } finally {
                reader.dispose()
            }
        }
    }.getOrNull()
}
