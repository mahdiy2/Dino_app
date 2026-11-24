package com.dino.message.chatfeature.presentation.component

import com.dino.message.corefeature.domain.upload.AttachmentUploadPolicy
import com.dino.message.corefeature.domain.upload.MediaFileHandle
import com.dino.message.corefeature.domain.upload.ReadableMediaFile
import com.dino.message.corefeature.domain.upload.UploadInputProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.io.Buffer
import kotlin.coroutines.resume
import kotlin.js.JsAny

class BrowserAudioRecorder : PlatformAudioRecorder {
    private var session: JsAny? = null
    private var generation = 0L

    override val isRecording: Boolean get() = session != null
    override val currentDurationMs: Long get() = session?.let(::recordingDuration)?.toLong() ?: 0

    override suspend fun startRecording(): Boolean {
        if (session != null) return false
        val attempt = ++generation
        return suspendCancellableCoroutine { continuation ->
            startCapture(AttachmentUploadPolicy.MAX_AUDIO_SIZE_BYTES.toInt(), { capture ->
                if (!continuation.isActive || attempt != generation) {
                    cancelCapture(capture)
                    if (continuation.isActive) continuation.resume(false)
                } else {
                    session = capture
                    continuation.resume(true)
                }
            }, {
                if (continuation.isActive) continuation.resume(false)
            })
        }
    }

    override suspend fun stopRecording(): RecordedAudio? {
        val capture = session ?: return null
        val duration = currentDurationMs
        session = null
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { cancelCapture(capture) }
            finishCapture(capture, { id, extension, mime, array, size ->
                if (!continuation.isActive) return@finishCapture
                var bytes: ByteArray? = ByteArray(size) { captureByte(array, it).toByte() }
                continuation.resume(RecordedAudio(
                    ReadableMediaFile(
                        MediaFileHandle(id, "voice_$id.$extension", mime, size.toLong()),
                        UploadInputProvider { Buffer().apply { write(checkNotNull(bytes)) } },
                        release = { bytes = null },
                    ),
                    duration,
                ))
            }, {
                if (continuation.isActive) continuation.resume(null)
            })
        }
    }

    override fun cancelRecording() {
        generation++
        session?.let(::cancelCapture)
        session = null
    }

    override fun release() = cancelRecording()
}

@JsFun("""
(maxBytes, ready, failed) => {
    (async () => {
        const context = new AudioContext();
        let stream;
        try {
            await context.resume();
            stream = await navigator.mediaDevices.getUserMedia({audio: true});
            const session = {stream, context, started: performance.now(), ended: null, parts: [], bytes: 0};
            if (typeof MediaRecorder !== "undefined" && MediaRecorder.isTypeSupported("audio/mp4;codecs=mp4a.40.2")) {
                const recorder = new MediaRecorder(stream, {mimeType: "audio/mp4;codecs=mp4a.40.2", audioBitsPerSecond: 96000});
                session.recorder = recorder;
                session.done = new Promise(resolve => {
                    recorder.onstop = () => resolve(!session.failed);
                    recorder.onerror = () => { session.failed = true; resolve(false); };
                });
                recorder.ondataavailable = ({data}) => {
                    if (session.bytes + data.size <= maxBytes) {
                        session.parts.push(data);
                        session.bytes += data.size;
                    } else {
                        session.failed = true;
                    }
                    if (session.bytes >= maxBytes * 0.95 || session.failed) {
                        if (recorder.state !== "inactive") recorder.stop();
                        session.ended ??= performance.now();
                        stream.getTracks().forEach(track => track.stop());
                    }
                };
                recorder.start(250);
            } else {
                const sourceCode = "class Capture extends AudioWorkletProcessor {" +
                    "process(inputs) { const input = inputs[0][0]; if(input) {" +
                    "const pcm = Int16Array.from(input, value => Math.max(-1, Math.min(1, value)) * 32767);" +
                    "this.port.postMessage(pcm.buffer, [pcm.buffer]); } return true; }} registerProcessor('capture-pcm', Capture);";
                const workletUrl = URL.createObjectURL(new Blob([sourceCode], {type: "text/javascript"}));
                try { await context.audioWorklet.addModule(workletUrl); } finally { URL.revokeObjectURL(workletUrl); }
                session.source = context.createMediaStreamSource(stream);
                session.node = new AudioWorkletNode(context, "capture-pcm", {channelCount: 1});
                session.source.connect(session.node);
                session.node.connect(context.destination);
                session.node.port.onmessage = ({data}) => {
                    if (session.ended !== null) return;
                    if (session.bytes + data.byteLength + 44 <= maxBytes) {
                        session.parts.push(data);
                        session.bytes += data.byteLength;
                    } else {
                        session.ended = performance.now();
                        session.source.disconnect();
                        session.node.disconnect();
                        stream.getTracks().forEach(track => track.stop());
                    }
                };
            }
            return session;
        } catch (error) {
            stream?.getTracks().forEach(track => track.stop());
            await context.close();
            throw error;
        }
    })().then(ready, failed);
}
""")
private external fun startCapture(maxBytes: Int, ready: (JsAny) -> Unit, failed: () -> Unit)

@JsFun("""
(session, ready, failed) => {
    (async () => {
        session.ended ??= performance.now();
        session.stream.getTracks().forEach(track => track.stop());
        let blob;
        try {
            if (session.recorder) {
                if (session.recorder.state !== "inactive") session.recorder.stop();
                if (!await session.done) throw new Error("Recording failed");
                blob = new Blob(session.parts, {type: "audio/mp4"});
            } else {
                session.source.disconnect();
                session.node.disconnect();
                const header = new DataView(new ArrayBuffer(44));
                const tag = (offset, text) => [...text].forEach((letter, index) => header.setUint8(offset + index, letter.charCodeAt(0)));
                tag(0, "RIFF"); header.setUint32(4, 36 + session.bytes, true);
                tag(8, "WAVE"); tag(12, "fmt "); header.setUint32(16, 16, true);
                header.setUint16(20, 1, true); header.setUint16(22, 1, true);
                header.setUint32(24, session.context.sampleRate, true);
                header.setUint32(28, session.context.sampleRate * 2, true);
                header.setUint16(32, 2, true); header.setUint16(34, 16, true);
                tag(36, "data"); header.setUint32(40, session.bytes, true);
                blob = new Blob([header.buffer, ...session.parts], {type: "audio/wav"});
            }
        } finally {
            if (session.context.state !== "closed") await session.context.close();
        }
        if (session.bytes === 0) throw new Error("Empty recording");
        return [crypto.randomUUID(), session.recorder ? "m4a" : "wav", blob.type, new Uint8Array(await blob.arrayBuffer()), blob.size];
    })().then(result => ready(...result), failed);
}
""")
private external fun finishCapture(
    session: JsAny,
    ready: (String, String, String, JsAny, Int) -> Unit,
    failed: () -> Unit,
)

@JsFun("""
(session) => {
    session.ended ??= performance.now();
    if (session.recorder && session.recorder.state !== "inactive") session.recorder.stop();
    session.stream.getTracks().forEach(track => track.stop());
    if (session.context.state !== "closed") session.context.close().catch(() => {});
    session.parts.length = 0;
}
""")
private external fun cancelCapture(session: JsAny)

@JsFun("(session) => (session.ended ?? performance.now()) - session.started")
private external fun recordingDuration(session: JsAny): Double

@JsFun("(array, index) => array[index]")
private external fun captureByte(array: JsAny, index: Int): Int
