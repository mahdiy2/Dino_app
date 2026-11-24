package com.dino.message.corefeature.data.upload

import com.dino.message.corefeature.domain.upload.MediaFileHandle
import com.dino.message.corefeature.domain.upload.PlatformMediaMetadata
import com.dino.message.corefeature.domain.upload.ReadableMediaFile
import com.dino.message.corefeature.domain.upload.UploadInputProvider
import com.dino.message.corefeature.domain.upload.UploadMediaCategory
import com.dino.message.corefeature.domain.upload.UploadMediaPicker
import com.dino.message.corefeature.domain.upload.resolveMediaFileMimeType
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.buffered
import platform.AVFoundation.AVAssetImageGenerator
import platform.AVFoundation.AVMetadataCommonKeyArtist
import platform.AVFoundation.AVMetadataCommonKeyTitle
import platform.AVFoundation.AVMetadataItem
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.commonKey
import platform.AVFoundation.commonMetadata
import platform.AVFoundation.stringValue
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSInputStream
import platform.Foundation.NSURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeAudio
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.UniformTypeIdentifiers.UTTypeMovie
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
class IosUploadMediaPicker(private val presenter: UIViewController) : UploadMediaPicker {
    private var delegate: UIDocumentPickerDelegateProtocol? = null

    override fun pick(
        categories: Set<UploadMediaCategory>,
        onResult: (Result<ReadableMediaFile?>) -> Unit,
    ) {
        check(delegate == null) { "A media selection is already pending" }
        val types = categories.map {
            when (it) {
                UploadMediaCategory.Image -> UTTypeImage
                UploadMediaCategory.Video -> UTTypeMovie
                UploadMediaCategory.Audio -> UTTypeAudio
            }
        }
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = types, asCopy = true)
        delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
            override fun documentPicker(
                controller: UIDocumentPickerViewController,
                didPickDocumentsAtURLs: List<*>,
            ) {
                val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                dispatch_async(dispatch_get_global_queue(0, 0uL)) {
                    val result = runCatching { url?.let(::readImportedFile) }
                    if (result.isFailure && url != null) NSFileManager.defaultManager.removeItemAtURL(url, null)
                    dispatch_async(dispatch_get_main_queue()) {
                        delegate = null
                        onResult(result)
                    }
                }
            }

            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                delegate = null
                onResult(Result.success(null))
            }
        }
        picker.delegate = delegate
        picker.allowsMultipleSelection = false
        presenter.presentViewController(picker, animated = true, completion = null)
    }

    private fun readImportedFile(url: NSURL): ReadableMediaFile {
        val path = checkNotNull(url.path)
        val name = url.lastPathComponent ?: "file"
        val mime = resolveMediaFileMimeType(
            name,
            UTType.typeWithFilenameExtension(url.pathExtension.orEmpty())?.preferredMIMEType,
        )
        val size = (NSFileManager.defaultManager.attributesOfItemAtPath(path, null)
            ?.get(NSFileSize) as? Number)?.toLong()
        val asset = if (mime.startsWith("audio/") || mime.startsWith("video/")) {
            AVURLAsset(url, null)
        } else {
            null
        }
        val tags = asset?.commonMetadata?.filterIsInstance<AVMetadataItem>().orEmpty()
        val duration = asset?.let { CMTimeGetSeconds(it.duration) }
            ?.takeIf { it.isFinite() && it >= 0 }
            ?.let { (it * 1_000).toLong() }
        val thumbnail = when {
            mime.startsWith("video/") && asset != null -> videoThumbnail(asset)
            mime.startsWith("image/") -> imageThumbnail(path)
            else -> null
        }
        return ReadableMediaFile(
            handle = MediaFileHandle(url.absoluteString.orEmpty(), name, mime, size),
            openInput = UploadInputProvider {
                check(NSFileManager.defaultManager.fileExistsAtPath(path)) { "Imported file is unavailable" }
                IosFileSource(url).buffered()
            },
            release = { NSFileManager.defaultManager.removeItemAtURL(url, null) },
            metadata = PlatformMediaMetadata(
                durationMs = duration,
                title = tags.firstOrNull { it.commonKey == AVMetadataCommonKeyTitle }?.stringValue,
                artist = tags.firstOrNull { it.commonKey == AVMetadataCommonKeyArtist }?.stringValue,
                mimeType = mime,
                sizeBytes = size,
                thumbnailJpeg = thumbnail,
            ),
        )
    }

    private fun videoThumbnail(asset: AVURLAsset): ByteArray? {
        val generator = AVAssetImageGenerator(asset).apply {
            appliesPreferredTrackTransform = true
            maximumSize = CGSizeMake(320.0, 320.0)
        }
        val image = generator.copyCGImageAtTime(CMTimeMake(0L, 1), null, null) ?: return null
        return try {
            UIImageJPEGRepresentation(UIImage.imageWithCGImage(image), 0.85)?.copyBytes()
        } finally {
            CGImageRelease(image)
        }
    }

    private fun imageThumbnail(path: String): ByteArray? {
        val image = UIImage.imageWithContentsOfFile(path) ?: return null
        val (width, height) = image.size.useContents { width to height }
        if (width <= 0 || height <= 0) return null
        val scale = minOf(1.0, 320.0 / maxOf(width, height))
        val targetWidth = (width * scale).coerceAtLeast(1.0)
        val targetHeight = (height * scale).coerceAtLeast(1.0)
        UIGraphicsBeginImageContextWithOptions(CGSizeMake(targetWidth, targetHeight), true, 1.0)
        return try {
            image.drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
            UIGraphicsGetImageFromCurrentImageContext()
                ?.let { UIImageJPEGRepresentation(it, 0.85) }
                ?.copyBytes()
        } finally {
            UIGraphicsEndImageContext()
        }
    }

    private fun NSData.copyBytes(): ByteArray = ByteArray(length.toInt()).also { result ->
        if (result.isNotEmpty()) result.usePinned { memcpy(it.addressOf(0), bytes, length) }
    }
}

@OptIn(ExperimentalForeignApi::class)
internal class IosFileSource(url: NSURL) : RawSource {
    private val stream = NSInputStream(url).apply { open() }
    private val buffer = ByteArray(16 * 1024)

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        require(byteCount >= 0)
        if (byteCount == 0L) return 0
        val count = buffer.usePinned {
            stream.read(it.addressOf(0).reinterpret(), minOf(byteCount, buffer.size.toLong()).toULong())
        }
        check(count >= 0) { stream.streamError?.localizedDescription ?: "Could not read imported media" }
        if (count == 0L) return -1
        sink.write(buffer, endIndex = count.toInt())
        return count
    }

    override fun close() = stream.close()
}
