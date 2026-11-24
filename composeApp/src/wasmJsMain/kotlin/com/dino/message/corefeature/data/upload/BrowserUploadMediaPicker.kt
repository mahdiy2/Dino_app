package com.dino.message.corefeature.data.upload

import com.dino.message.corefeature.domain.upload.MediaFileHandle
import com.dino.message.corefeature.domain.upload.PlatformMediaMetadata
import com.dino.message.corefeature.domain.upload.ReadableMediaFile
import com.dino.message.corefeature.domain.upload.UploadInputProvider
import com.dino.message.corefeature.domain.upload.UploadMediaCategory
import com.dino.message.corefeature.domain.upload.UploadMediaPicker
import com.dino.message.corefeature.domain.upload.resolveMediaFileMimeType
import kotlinx.io.Buffer
import kotlin.js.JsAny

class BrowserUploadMediaPicker : UploadMediaPicker {
    override fun pick(
        categories: Set<UploadMediaCategory>,
        onResult: (Result<ReadableMediaFile?>) -> Unit,
    ) {
        openMediaInput(
            accept = categories.joinToString(",") { "${it.mimePrefix}/*" },
            picked = { blob, id, name, reportedMime, size ->
                val mime = resolveMediaFileMimeType(name, reportedMime)
                if (size > Int.MAX_VALUE) {
                    onResult(Result.failure(IllegalArgumentException("Selected file is too large to read")))
                } else {
                    readBrowserMedia(
                        blob, mime,
                        ready = { data, duration, thumbnail ->
                            val sizeBytes = size.toLong()
                            var bytes: ByteArray? = data.toByteArray()
                            onResult(Result.success(ReadableMediaFile(
                                handle = MediaFileHandle(id, name, mime, sizeBytes),
                                openInput = UploadInputProvider {
                                    Buffer().apply { write(checkNotNull(bytes) { "File released" }) }
                                },
                                metadata = PlatformMediaMetadata(
                                    durationMs = duration.takeIf { it >= 0 }?.toLong(),
                                    mimeType = mime,
                                    sizeBytes = sizeBytes,
                                    thumbnailJpeg = thumbnail?.toByteArray(),
                                ),
                                release = { bytes = null },
                            )))
                        },
                        failed = { onResult(Result.failure(IllegalStateException(it))) },
                    )
                }
            },
            cancelled = { onResult(Result.success(null)) },
            failed = { onResult(Result.failure(IllegalStateException(it))) },
        )
    }
}

private fun JsAny.toByteArray(): ByteArray = ByteArray(byteLength(this)) { byteAt(this, it).toByte() }

@JsFun("(bytes) => bytes.length")
private external fun byteLength(bytes: JsAny): Int

@JsFun("(bytes, index) => bytes[index]")
private external fun byteAt(bytes: JsAny, index: Int): Int

@JsFun("""
(accept, picked, cancelled, failed) => {
    const input = document.createElement("input");
    input.type = "file";
    input.accept = accept;
    input.hidden = true;
    input.addEventListener("cancel", () => {
        input.remove();
        cancelled();
    }, { once: true });
    input.addEventListener("change", () => {
        const file = input.files[0];
        input.remove();
        if (file) {
            const id = URL.createObjectURL(file);
            URL.revokeObjectURL(id);
            picked(file, id, file.name, file.type, file.size);
        } else {
            cancelled();
        }
    }, { once: true });
    try {
        document.body.appendChild(input);
        if (typeof input.showPicker === "function") input.showPicker();
        else input.click();
    } catch (error) {
        input.remove();
        failed(String(error));
    }
}
""")
private external fun openMediaInput(
    accept: String,
    picked: (JsAny, String, String, String, Double) -> Unit,
    cancelled: () -> Unit,
    failed: (String) -> Unit,
)

@JsFun("""
(file, mime, ready, failed) => {
    const preview = new Promise(resolve => {
        if (!mime.startsWith("image/") && !mime.startsWith("video/") && !mime.startsWith("audio/")) {
            resolve([-1, null]);
            return;
        }
        const url = URL.createObjectURL(file);
        const image = mime.startsWith("image/");
        const video = mime.startsWith("video/");
        const media = document.createElement(image ? "img" : video ? "video" : "audio");
        let duration = -1;
        let finished = false;
        const complete = thumbnail => {
            if (finished) return;
            finished = true;
            clearTimeout(timeout);
            media.removeAttribute("src");
            if (!image) media.load();
            URL.revokeObjectURL(url);
            resolve([duration, thumbnail]);
        };
        const timeout = setTimeout(() => complete(null), 3000);
        const frame = async (width, height) => {
            if (finished || !width || !height) return complete(null);
            try {
                const canvas = document.createElement("canvas");
                const scale = Math.min(1, 320 / Math.max(width, height));
                canvas.width = Math.max(1, Math.round(width * scale));
                canvas.height = Math.max(1, Math.round(height * scale));
                canvas.getContext("2d").drawImage(media, 0, 0, canvas.width, canvas.height);
                const blob = await new Promise(done => canvas.toBlob(done, "image/jpeg", 0.85));
                complete(blob ? new Uint8Array(await blob.arrayBuffer()) : null);
            } catch (_) {
                complete(null);
            }
        };
        media.onerror = () => complete(null);
        if (image) {
            media.onload = () => frame(media.naturalWidth, media.naturalHeight);
        } else {
            media.preload = video ? "auto" : "metadata";
            media.onloadedmetadata = () => {
                if (Number.isFinite(media.duration)) duration = media.duration * 1000;
                if (!video) complete(null);
            };
            if (video) media.onloadeddata = () => frame(media.videoWidth, media.videoHeight);
        }
        media.src = url;
    });
    Promise.all([file.arrayBuffer(), preview]).then(
        ([data, [duration, thumbnail]]) => ready(new Uint8Array(data), duration, thumbnail),
        error => failed(String(error)),
    );
}
""")
private external fun readBrowserMedia(
    file: JsAny,
    mime: String,
    ready: (JsAny, Double, JsAny?) -> Unit,
    failed: (String) -> Unit,
)
