package com.dino.message.threadfeature

import android.Manifest
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dino.message.chatfeature.presentation.component.AndroidVoiceRecorder
import com.dino.message.corefeature.data.upload.AndroidUploadContentProvider
import com.dino.message.threadfeature.domain.model.CreatePostMedia
import com.dino.message.threadfeature.presentation.component.exportEditedStoryVideo
import com.dino.message.threadfeature.presentation.music.MusicPlaybackManager
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidMediaRuntimeTest {
    @Test
    fun servicePlaysLocalAudioAndPublishesProgress() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val wav = File.createTempFile("media3-smoke", ".wav", context.cacheDir)
        val samples = 48_000
        val pcmSize = samples * 2
        wav.writeBytes(ByteBuffer.allocate(44 + pcmSize).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".encodeToByteArray())
            putInt(36 + pcmSize)
            put("WAVEfmt ".encodeToByteArray())
            putInt(16)
            putShort(1)
            putShort(1)
            putInt(16_000)
            putInt(32_000)
            putShort(2)
            putShort(16)
            put("data".encodeToByteArray())
            putInt(pcmSize)
        }.array())
        try {
            withContext(Dispatchers.Main) {
                MusicPlaybackManager.playTrack(Uri.fromFile(wav).toString(), "Smoke fixture", null)
            }
            val state = withTimeout(15_000) {
                MusicPlaybackManager.playbackState.first { it.durationMs > 0 && it.positionMs > 0 }
            }
            assertTrue(state.durationMs in 2_900..3_100)
        } finally {
            withContext(Dispatchers.Main) { MusicPlaybackManager.stop() }
            wav.delete()
        }
    }

    @Test
    fun recorderProducesReadableAacAndReleasesOutput() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.RECORD_AUDIO)
        withContext(Dispatchers.Main) {
            val recorder = AndroidVoiceRecorder(context)
            try {
                assertTrue(recorder.startRecording())
                delay(1_000)
                val recording = checkNotNull(recorder.stopRecording())
                try {
                    assertTrue(recording.durationMs >= 500)
                    assertTrue((recording.file.handle.sizeBytes ?: 0) > 0)
                    recording.file.openInput.open().use { assertTrue(!it.exhausted()) }
                } finally {
                    recording.file.release()
                }
                assertTrue(!File(checkNotNull(Uri.parse(recording.file.handle.id).path)).exists())
            } finally {
                recorder.release()
            }
        }
    }

    @Test
    fun transformerExportsAStoryVideoWithEffects() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val input = File.createTempFile("story-smoke", ".mp4", context.cacheDir)
        try {
            instrumentation.context.assets.open("story-smoke.mp4").use { source ->
                input.outputStream().use { target -> source.copyTo(target) }
            }
            val source = CreatePostMedia(
                AndroidUploadContentProvider(context.contentResolver).readableMediaFile(
                    Uri.fromFile(input), input.name, "video/mp4", input.length(),
                ),
            )
            val exported = withTimeout(45_000) {
                exportEditedStoryVideo(
                    context, source, IntSize(160, 288), 1.15f, Offset.Zero,
                    emptyList(), emptyList(), emptyList(),
                )
            }
            try {
                assertNotNull(exported)
                assertTrue((exported?.sizeBytes ?: 0) > 0)
            } finally {
                exported?.file?.release?.invoke()
            }
        } finally {
            input.delete()
        }
    }
}
