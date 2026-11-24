package com.dino.message.chatfeature.presentation.component

import com.dino.message.corefeature.data.upload.IosFileSource
import com.dino.message.corefeature.domain.upload.AttachmentUploadPolicy
import com.dino.message.corefeature.domain.upload.MediaFileHandle
import com.dino.message.corefeature.domain.upload.ReadableMediaFile
import com.dino.message.corefeature.domain.upload.UploadInputProvider
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.io.buffered
import platform.AVFAudio.*
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosAudioRecorder : PlatformAudioRecorder {
    private val session = AVAudioSession.sharedInstance()
    private var recorder: AVAudioRecorder? = null
    private var output: NSURL? = null
    private var generation = 0L

    override val isRecording: Boolean get() = recorder != null
    override val currentDurationMs: Long get() = ((recorder?.currentTime ?: 0.0) * 1_000).toLong()

    override suspend fun startRecording(): Boolean {
        if (recorder != null) return false
        val attempt = ++generation
        val granted = suspendCancellableCoroutine { continuation ->
            session.requestRecordPermission { continuation.resume(it) }
        }
        if (!granted || attempt != generation) return false
        if (!session.setCategory(AVAudioSessionCategoryPlayAndRecord, null) ||
            !session.setActive(true, null)
        ) return false
        val name = "voice_${NSUUID().UUIDString}.m4a"
        val url = NSURL.fileURLWithPath("${NSTemporaryDirectory().trimEnd('/')}/$name")
        return runCatching {
            val next = AVAudioRecorder(
                url,
                mapOf(
                    AVFormatIDKey to AAC_FORMAT_ID,
                    AVSampleRateKey to 44_100,
                    AVNumberOfChannelsKey to 1,
                    AVEncoderBitRateKey to BIT_RATE,
                ),
                null,
            )
            val maxSeconds = AttachmentUploadPolicy.MAX_AUDIO_SIZE_BYTES * 8.0 / BIT_RATE
            check(next.recordForDuration(maxSeconds * 0.95))
            output = url
            recorder = next
        }.onFailure {
            NSFileManager.defaultManager.removeItemAtURL(url, null)
            session.setActive(false, null)
        }.isSuccess
    }

    override suspend fun stopRecording(): RecordedAudio? {
        val url = output ?: return null
        val duration = currentDurationMs
        finish()
        val path = checkNotNull(url.path)
        val manager = NSFileManager.defaultManager
        val size = (manager.attributesOfItemAtPath(path, null)?.get(NSFileSize) as? Number)?.toLong()
        if (size == null || size == 0L || size > AttachmentUploadPolicy.MAX_AUDIO_SIZE_BYTES) {
            manager.removeItemAtURL(url, null)
            return null
        }
        return RecordedAudio(
            ReadableMediaFile(
                MediaFileHandle(url.absoluteString.orEmpty(), url.lastPathComponent.orEmpty(), "audio/mp4", size),
                UploadInputProvider { IosFileSource(url).buffered() },
                release = { manager.removeItemAtURL(url, null) },
            ),
            duration,
        )
    }

    override fun cancelRecording() {
        generation++
        val url = output ?: return
        finish()
        NSFileManager.defaultManager.removeItemAtURL(url, null)
    }

    override fun release() = cancelRecording()

    private fun finish() {
        recorder?.stop()
        recorder = null
        output = null
        session.setActive(false, null)
    }

    private companion object {
        const val AAC_FORMAT_ID = 0x61616320 // Core Audio FourCC: aac followed by a space.
        const val BIT_RATE = 96_000
    }
}
