package com.dino.message.chatfeature.presentation.component

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

class VoiceRecorderManager(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTimeMs: Long = 0L

    val isRecording: Boolean get() = recorder != null

    val currentDurationMs: Long
        get() = if (isRecording) System.currentTimeMillis() - startTimeMs else 0L

    fun startRecording(): Boolean {
        if (recorder != null) return false

        val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        outputFile = file

        val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        return try {
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioSamplingRate(44100)
            mr.setAudioEncodingBitRate(96000)
            mr.setOutputFile(file.absolutePath)
            mr.prepare()
            mr.start()
            startTimeMs = System.currentTimeMillis()
            recorder = mr
            true
        } catch (e: Exception) {
            mr.release()
            outputFile?.delete()
            outputFile = null
            false
        }
    }

    fun stopRecording(): Uri? {
        val mr = recorder ?: return null
        return try {
            mr.stop()
            mr.release()
            recorder = null
            val file = outputFile ?: return null
            if (!file.exists() || file.length() == 0L) {
                file.delete()
                outputFile = null
                return null
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            mr.release()
            recorder = null
            outputFile?.delete()
            outputFile = null
            null
        }
    }

    fun cancelRecording() {
        val mr = recorder ?: return
        try {
            mr.stop()
        } catch (_: Exception) { }
        mr.release()
        recorder = null
        outputFile?.delete()
        outputFile = null
    }

    fun release() {
        cancelRecording()
    }
}
