package com.dino.message.chatfeature.presentation.component

import com.dino.message.corefeature.domain.upload.ReadableMediaFile

data class RecordedAudio(val file: ReadableMediaFile, val durationMs: Long)

interface PlatformAudioRecorder {
    val isRecording: Boolean
    val currentDurationMs: Long

    suspend fun startRecording(): Boolean
    suspend fun stopRecording(): RecordedAudio?
    fun cancelRecording()
    fun release()
}

fun interface PlatformAudioRecorderFactory {
    fun createRecorder(): PlatformAudioRecorder
}
