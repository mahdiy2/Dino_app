package com.dino.message.threadfeature.presentation.music

import kotlin.js.JsAny

/** When a host element is supplied, render an inline video surface inside it. */
class BrowserMediaPlaybackEngine(private val hostElementId: String? = null) : MediaPlaybackEngine {
    private var player: JsAny? = null

    override fun load(url: String, onEvent: (PlaybackEngineEvent) -> Unit) {
        close()
        player = createPlayer(
            url, hostElementId,
            { position, duration -> onEvent(PlaybackEngineEvent.Progress(position.toLong(), duration.toLong())) },
            { playing, buffering -> onEvent(PlaybackEngineEvent.Status(playing, buffering)) },
            { error -> onEvent(PlaybackEngineEvent.Failure(error)) },
            { onEvent(PlaybackEngineEvent.Ended) },
        )
    }

    override fun play() { player?.let { command(it, "play", 0.0) } }
    override fun pause() { player?.let { command(it, "pause", 0.0) } }
    override fun seekTo(positionMs: Long) { player?.let { command(it, "seek", positionMs.toDouble()) } }

    override fun close() {
        val previous = player ?: return
        player = null
        command(previous, "close", 0.0)
    }
}

@JsFun("""
(url, hostId, progress, status, failure, ended) => {
    const host = hostId === null ? null : document.getElementById(hostId);
    const media = document.createElement(host ? "video" : "audio");
    const listeners = new AbortController();
    const listen = (name, callback) => media.addEventListener(name, callback, {signal: listeners.signal});
    const reportTime = () => progress(media.currentTime * 1000,
        Number.isFinite(media.duration) ? media.duration * 1000 : 0);
    listen("loadedmetadata", reportTime);
    listen("durationchange", reportTime);
    listen("timeupdate", reportTime);
    listen("playing", () => status(true, false));
    listen("pause", () => status(false, false));
    listen("waiting", () => status(!media.paused, true));
    listen("canplay", () => status(!media.paused, false));
    listen("ended", ended);
    listen("error", () => failure(media.error?.message || "Unable to play this media"));
    media.preload = "metadata";
    media.playsInline = true;
    if (host) {
        media.style.width = "100%";
        media.style.height = "100%";
        host.appendChild(media);
    }
    media.src = url;
    return {media, listeners, failure};
}
""")
private external fun createPlayer(
    url: String,
    hostId: String?,
    progress: (Double, Double) -> Unit,
    status: (Boolean, Boolean) -> Unit,
    failure: (String) -> Unit,
    ended: () -> Unit,
): JsAny

@JsFun("""
(player, action, position) => {
    const {media, listeners, failure} = player;
    if (action === "play") {
        media.play().catch(error => {
            if (!listeners.signal.aborted && error.name !== "AbortError") failure(error.message);
        });
    } else if (action === "pause") {
        media.pause();
    } else if (action === "seek") {
        media.currentTime = position / 1000;
    } else {
        listeners.abort();
        media.pause();
        media.removeAttribute("src");
        media.load();
        media.remove();
    }
}
""")
private external fun command(player: JsAny, action: String, position: Double)
