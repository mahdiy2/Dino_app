package com.dino.message.threadfeature.presentation.music

import com.dino.message.chatfeature.presentation.component.IosAudioRecorder
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.create
import platform.Foundation.writeToURL
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosMediaRuntimeSmokeTest {
    @Test
    fun avPlayerPlaysLocalPcmMedia() = runTest {
        if (!enabled("DINO_MEDIA_SMOKE")) return@runTest
        withContext(Dispatchers.Main) {
            val fixture = NSURL.fileURLWithPath("${NSTemporaryDirectory()}/dino-${NSUUID().UUIDString}.wav")
            val data = waveFixture().usePinned { bytes ->
                NSData.create(bytes = bytes.addressOf(0), length = 16_044uL)
            }
            check(data.writeToURL(fixture, atomically = true))
            val events = Channel<PlaybackEngineEvent>(Channel.UNLIMITED)
            val player = IosMediaPlaybackEngine(this)
            try {
                player.load(checkNotNull(fixture.absoluteString)) { events.trySend(it) }
                player.play()
                withTimeout(15_000) {
                    var sawDuration = false
                    while (true) {
                        when (val event = events.receive()) {
                            is PlaybackEngineEvent.Progress -> sawDuration = sawDuration || event.durationMs in 450..550
                            is PlaybackEngineEvent.Failure -> error(event.message)
                            PlaybackEngineEvent.Ended -> {
                                assertTrue(sawDuration)
                                return@withTimeout
                            }
                            else -> Unit
                        }
                    }
                }
            } finally {
                player.close()
                NSFileManager.defaultManager.removeItemAtURL(fixture, null)
            }
        }
    }

    @Test
    fun avAudioRecorderProducesReadableMedia() = runTest {
        if (!enabled("DINO_MEDIA_MIC_SMOKE")) return@runTest
        withContext(Dispatchers.Main) {
            val recorder = IosAudioRecorder()
            try {
                assertTrue(recorder.startRecording())
                delay(1_000)
                val recording = assertNotNull(recorder.stopRecording())
                try {
                    assertTrue(recording.durationMs >= 500)
                    recording.file.openInput.open().use { assertTrue(!it.exhausted()) }
                } finally {
                    recording.file.release()
                }
            } finally {
                recorder.release()
            }
        }
    }

    private fun enabled(key: String) = NSProcessInfo.processInfo.environment[key] == "1"

    private fun waveFixture(): ByteArray {
        val bytes = ByteArray(16_044)
        fun word(offset: Int, value: Int, size: Int = 4) {
            repeat(size) { index -> bytes[offset + index] = (value shr (index * 8)).toByte() }
        }
        fun tag(offset: Int, value: String) {
            value.encodeToByteArray().copyInto(bytes, offset)
        }
        tag(0, "RIFF")
        word(4, 16_036)
        tag(8, "WAVEfmt ")
        word(16, 16)
        word(20, 1, 2)
        word(22, 1, 2)
        word(24, 16_000)
        word(28, 32_000)
        word(32, 2, 2)
        word(34, 16, 2)
        tag(36, "data")
        word(40, 16_000)
        return bytes
    }
}
