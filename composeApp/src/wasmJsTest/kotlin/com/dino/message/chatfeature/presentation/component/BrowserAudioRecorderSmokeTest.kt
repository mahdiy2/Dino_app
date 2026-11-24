package com.dino.message.chatfeature.presentation.component

import kotlin.coroutines.resume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest

class BrowserAudioRecorderSmokeTest {
    @Test
    fun capturesAndReleasesFakeMicrophone() = runTest {
        if (!hasFakeMicrophone()) return@runTest
        setForcePcm(true)
        val recorder = BrowserAudioRecorder()
        try {
            assertTrue(recorder.startRecording())
            suspendCancellableCoroutine { continuation ->
                waitForAudio { if (continuation.isActive) continuation.resume(Unit) }
            }
            val recorded = assertNotNull(recorder.stopRecording())
            try {
                assertEquals("audio/wav", recorded.file.handle.mimeType)
                assertTrue(recorded.durationMs > 0)
                assertTrue((recorded.file.handle.sizeBytes ?: 0) > 44)
                recorded.file.openInput.open().use { assertTrue(!it.exhausted()) }
            } finally {
                recorded.file.release()
            }
        } finally {
            recorder.release()
            setForcePcm(false)
        }
    }
}

@JsFun("() => navigator.userAgent.includes('DinoMediaSmoke')")
private external fun hasFakeMicrophone(): Boolean

@JsFun("(callback) => setTimeout(callback, 600)")
private external fun waitForAudio(callback: () -> Unit)

@JsFun("""
(force) => {
    if (force) {
        globalThis.originalAudioFormatSupport = MediaRecorder.isTypeSupported;
        MediaRecorder.isTypeSupported = () => false;
    } else {
        MediaRecorder.isTypeSupported = globalThis.originalAudioFormatSupport;
        delete globalThis.originalAudioFormatSupport;
    }
}
""")
private external fun setForcePcm(force: Boolean)
