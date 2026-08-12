package com.dino.message.chatfeature.presentation.component

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

data class RecordedAudio(
    val source: String,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val durationMs: Long,
)

interface PlatformAudioRecorder {
    val isRecording: Boolean
    val currentDurationMs: Long

    fun startRecording(): Boolean
    fun stopRecording(): RecordedAudio?
    fun cancelRecording()
    fun release()
}

interface PlatformAudioRecorderFactory {
    fun createRecorder(): PlatformAudioRecorder
}

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

    override fun startRecording(): Boolean {
        if (recorder != null) return false

        val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
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

    override fun stopRecording(): RecordedAudio? {
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
                source = file.absolutePath,
                fileName = file.name,
                mimeType = VOICE_RECORDING_MIME_TYPE,
                sizeBytes = file.length(),
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
