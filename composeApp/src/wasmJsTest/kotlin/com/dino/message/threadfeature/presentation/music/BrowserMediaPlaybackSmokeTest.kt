package com.dino.message.threadfeature.presentation.music

import kotlin.coroutines.resume
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest

class BrowserMediaPlaybackSmokeTest {
    @Test
    fun browserReportsDurationForLocalPcmMedia() = runTest {
        val engine = BrowserMediaPlaybackEngine()
        val url = testWaveUrl()
        try {
            val duration = suspendCancellableCoroutine { continuation ->
                engine.load(url) { event ->
                    if (!continuation.isActive) return@load
                    when (event) {
                        is PlaybackEngineEvent.Progress ->
                            if (event.durationMs > 0) continuation.resume(event.durationMs)
                        is PlaybackEngineEvent.Failure -> continuation.cancel(IllegalStateException(event.message))
                        else -> Unit
                    }
                }
            }
            assertTrue(duration in 450..550)
        } finally {
            engine.close()
            releaseWaveUrl(url)
        }
    }
}

@JsFun("""
() => {
    const data = new DataView(new ArrayBuffer(16044));
    const tag = (offset, text) => [...text].forEach((character, index) =>
        data.setUint8(offset + index, character.charCodeAt(0)));
    tag(0, "RIFF"); data.setUint32(4, 16036, true);
    tag(8, "WAVE"); tag(12, "fmt "); data.setUint32(16, 16, true);
    data.setUint16(20, 1, true); data.setUint16(22, 1, true);
    data.setUint32(24, 16000, true); data.setUint32(28, 32000, true);
    data.setUint16(32, 2, true); data.setUint16(34, 16, true);
    tag(36, "data"); data.setUint32(40, 16000, true);
    return URL.createObjectURL(new Blob([data.buffer], {type: "audio/wav"}));
}
""")
private external fun testWaveUrl(): String

@JsFun("(url) => URL.revokeObjectURL(url)")
private external fun releaseWaveUrl(url: String)
