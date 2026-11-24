package com.dino.message.chatfeature.presentation.component

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.dino.message.corefeature.domain.upload.MediaFileHandle
import com.dino.message.corefeature.domain.upload.ReadableMediaFile
import com.dino.message.corefeature.domain.upload.UploadInputProvider
import io.ktor.utils.io.streams.asInput
import java.io.File

class AndroidAudioRecorderFactory(private val context: Context) : PlatformAudioRecorderFactory {
    override fun createRecorder(): PlatformAudioRecorder = AndroidVoiceRecorder(context)
}

class AndroidVoiceRecorder(private val context: Context) : PlatformAudioRecorder {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTimeMs: Long = 0L

    override val isRecording: Boolean get() = recorder != null

    override val currentDurationMs: Long
        get() = if (isRecording) System.currentTimeMillis() - startTimeMs else 0L

    override suspend fun startRecording(): Boolean {
        if (recorder != null) return false

        val file = File(context.cacheDir, "voice_${java.util.UUID.randomUUID()}.m4a")
        outputFile = file

        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        return try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setAudioSamplingRate(44100)
            mediaRecorder.setAudioEncodingBitRate(96000)
            mediaRecorder.setMaxFileSize(com.dino.message.corefeature.domain.upload.AttachmentUploadPolicy.MAX_AUDIO_SIZE_BYTES)
            mediaRecorder.setOutputFile(file.absolutePath)
            mediaRecorder.prepare()
            mediaRecorder.start()
            startTimeMs = System.currentTimeMillis()
            recorder = mediaRecorder
            true
        } catch (_: Exception) {
            mediaRecorder.release()
            deleteOutputFile()
            false
        }
    }

    override suspend fun stopRecording(): RecordedAudio? {
        val mediaRecorder = recorder ?: return null
        return try {
            mediaRecorder.stop()
            mediaRecorder.release()
            recorder = null
            val file = outputFile ?: return null
            val durationMs = (System.currentTimeMillis() - startTimeMs).coerceAtLeast(0L)
            outputFile = null
            startTimeMs = 0L
            if (!file.exists() || file.length() == 0L) {
                file.delete()
                return null
            }
            RecordedAudio(
                file = ReadableMediaFile(
                    handle = MediaFileHandle(file.toURI().toString(), file.name, VOICE_RECORDING_MIME_TYPE, file.length()),
                    openInput = UploadInputProvider { file.inputStream().asInput() },
                    release = { file.delete() },
                ),
                durationMs = durationMs,
            )
        } catch (_: Exception) {
            mediaRecorder.release()
            recorder = null
            deleteOutputFile()
            null
        }
    }

    override fun cancelRecording() {
        val mediaRecorder = recorder ?: return
        try {
            mediaRecorder.stop()
        } catch (_: Exception) {
            Unit
        }
        mediaRecorder.release()
        recorder = null
        deleteOutputFile()
    }

    override fun release() {
        cancelRecording()
    }

    private fun deleteOutputFile() {
        outputFile?.delete()
        outputFile = null
        startTimeMs = 0L
    }

    companion object {
        const val VOICE_RECORDING_MIME_TYPE = "audio/mp4"
    }
}
