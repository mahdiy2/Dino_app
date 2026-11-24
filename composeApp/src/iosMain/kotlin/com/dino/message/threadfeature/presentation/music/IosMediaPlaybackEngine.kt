package com.dino.message.threadfeature.presentation.music

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.AVFoundation.*
import platform.AVKit.AVPlayerViewController
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.darwin.NSObjectProtocol

@OptIn(ExperimentalForeignApi::class)
class IosMediaPlaybackEngine(
    private val mainScope: CoroutineScope,
    private val surface: AVPlayerViewController? = null,
) : MediaPlaybackEngine {
    private var player: AVPlayer? = null
    private var updates: Job? = null
    private var endObserver: NSObjectProtocol? = null

    override fun load(url: String, onEvent: (PlaybackEngineEvent) -> Unit) {
        close()
        val source = NSURL.URLWithString(url)
        if (source == null) {
            onEvent(PlaybackEngineEvent.Failure("Invalid media URL"))
            return
        }
        val next = AVPlayer.playerWithURL(source)
        player = next
        surface?.player = next
        val item = next.currentItem
        if (item != null) {
            endObserver = NSNotificationCenter.defaultCenter.addObserverForName(
                AVPlayerItemDidPlayToEndTimeNotification, item, NSOperationQueue.mainQueue,
            ) { onEvent(PlaybackEngineEvent.Ended) }
        }
        updates = mainScope.launch {
            while (true) {
                if (next.status == AVPlayerStatusFailed || item?.status == AVPlayerItemStatusFailed) {
                    onEvent(PlaybackEngineEvent.Failure("Unable to play this media"))
                    break
                }
                val duration = item?.duration?.let { CMTimeGetSeconds(it).toMilliseconds() } ?: 0
                onEvent(PlaybackEngineEvent.Progress(CMTimeGetSeconds(next.currentTime()).toMilliseconds(), duration))
                onEvent(PlaybackEngineEvent.Status(
                    playing = next.timeControlStatus == AVPlayerTimeControlStatusPlaying,
                    buffering = next.timeControlStatus == AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate,
                ))
                delay(250)
            }
        }
    }

    override fun play() { player?.play() }
    override fun pause() { player?.pause() }
    override fun seekTo(positionMs: Long) { player?.seekToTime(CMTimeMake(positionMs, 1_000)) }

    override fun close() {
        updates?.cancel()
        updates = null
        endObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        endObserver = null
        player?.pause()
        player?.replaceCurrentItemWithPlayerItem(null)
        player = null
        surface?.player = null
    }

    private fun Double.toMilliseconds(): Long = if (isFinite() && this > 0) (this * 1_000).toLong() else 0
}
