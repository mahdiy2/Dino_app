package com.dino.message.threadfeature.presentation.music

import java.awt.EventQueue
import java.nio.file.Files
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.test.Test
import kotlin.test.assertTrue

class DesktopMediaPlaybackSmokeTest {
    @Test
    fun javaFxDecodesAndPlaysLocalPcmMedia() {
        if (System.getenv("DINO_MEDIA_SMOKE") != "1") return
        val file = Files.createTempFile("dino-playback", ".wav")
        val format = AudioFormat(16_000f, 16, 1, true, false)
        AudioInputStream(ByteArray(16_000).inputStream(), format, 8_000).use {
            AudioSystem.write(it, AudioFileFormat.Type.WAVE, file.toFile())
        }
        val events = LinkedBlockingQueue<PlaybackEngineEvent>()
        var engine: DesktopMediaPlaybackEngine? = null
        try {
            EventQueue.invokeAndWait {
                val player = DesktopMediaPlaybackEngine()
                engine = player
                player.load(file.toUri().toString()) { events.add(it) }
                player.play()
            }
            var sawProgress = false
            val end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
            while (System.nanoTime() < end) {
                when (val event = events.poll(1, TimeUnit.SECONDS)) {
                    is PlaybackEngineEvent.Progress -> sawProgress = sawProgress || event.durationMs >= 490
                    is PlaybackEngineEvent.Failure -> error(event.message)
                    PlaybackEngineEvent.Ended -> {
                        assertTrue(sawProgress)
                        return
                    }
                    else -> Unit
                }
            }
            error("Timed out waiting for the WAV to finish")
        } finally {
            EventQueue.invokeAndWait { engine?.close() }
            Files.deleteIfExists(file)
        }
    }
}
