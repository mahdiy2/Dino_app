package com.dino.message.chatfeature.presentation.component

import com.dino.message.corefeature.domain.upload.AttachmentUploadPolicy
import com.dino.message.corefeature.domain.upload.MediaFileHandle
import com.dino.message.corefeature.domain.upload.ReadableMediaFile
import com.dino.message.corefeature.domain.upload.UploadInputProvider
import io.ktor.utils.io.streams.asInput
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.TargetDataLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.concurrent.thread

class DesktopAudioRecorder : PlatformAudioRecorder {
    private val format = AudioFormat(16_000f, 16, 1, true, false)
    private var capture: Capture? = null

    override val isRecording: Boolean get() = capture != null
    override val currentDurationMs: Long
        get() = capture?.let { duration(it.bytes.size()) } ?: 0L

    override suspend fun startRecording(): Boolean {
        if (capture != null) return false
        return runCatching {
            val line = AudioSystem.getTargetDataLine(format)
            try {
                line.open(format)
                line.start()
                capture = Capture(line)
            } catch (exception: Exception) {
                line.close()
                throw exception
            }
        }.isSuccess
    }

    override suspend fun stopRecording(): RecordedAudio? {
        val session = capture ?: return null
        capture = null
        session.line.close()
        return withContext(Dispatchers.IO) {
            session.worker.join()
            val pcm = session.bytes.toByteArray()
            if (pcm.isEmpty() || session.failed) return@withContext null
            val wave = ByteArrayOutputStream()
            AudioInputStream(pcm.inputStream(), format, pcm.size.toLong() / format.frameSize).use {
                AudioSystem.write(it, AudioFileFormat.Type.WAVE, wave)
            }
            var data: ByteArray? = wave.toByteArray()
            val id = UUID.randomUUID().toString()
            RecordedAudio(
                file = ReadableMediaFile(
                    handle = MediaFileHandle(id, "voice_${id}.wav", "audio/wav", wave.size().toLong()),
                    openInput = UploadInputProvider { ByteArrayInputStream(checkNotNull(data)).asInput() },
                    release = { data = null },
                ),
                durationMs = duration(pcm.size),
            )
        }
    }

    override fun cancelRecording() {
        val session = capture ?: return
        capture = null
        session.line.close()
    }

    override fun release() = cancelRecording()

    private fun duration(size: Int): Long = size.toLong() * 1_000 / (16_000 * format.frameSize)

    private inner class Capture(val line: TargetDataLine) {
        val bytes = ByteArrayOutputStream()
        @Volatile var failed = false
        val worker = thread(isDaemon = true, name = "desktop-audio-capture") {
            val buffer = ByteArray(4_096)
            val maxPcmBytes = AttachmentUploadPolicy.MAX_AUDIO_SIZE_BYTES.toInt() - 44
            try {
                while (line.isOpen && bytes.size() < maxPcmBytes) {
                    val count = line.read(buffer, 0, minOf(buffer.size, maxPcmBytes - bytes.size()))
                    if (count <= 0) break
                    bytes.write(buffer, 0, count)
                }
            } catch (_: Exception) {
                failed = line.isOpen
            } finally {
                line.close()
            }
        }
    }
}
