package com.dino.message.threadfeature.presentation.music

import java.awt.EventQueue
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.scene.media.MediaView
import javafx.util.Duration
import javax.swing.JComponent

/** Create and call from the Swing event dispatch thread. */
class DesktopMediaPlaybackEngine(showVideo: Boolean = false) : MediaPlaybackEngine {
    private val bridge = JFXPanel()
    val videoSurface: JComponent? = bridge.takeIf { showVideo }
    private var player: MediaPlayer? = null

    override fun load(url: String, onEvent: (PlaybackEngineEvent) -> Unit) {
        Platform.runLater {
            clearPlayer()
            fun report(event: PlaybackEngineEvent) = EventQueue.invokeLater { onEvent(event) }
            try {
                val media = Media(url)
                val next = MediaPlayer(media)
                player = next
                if (videoSurface != null) {
                    val view = MediaView(next)
                    val container = StackPane(view)
                    view.fitWidthProperty().bind(container.widthProperty())
                    view.fitHeightProperty().bind(container.heightProperty())
                    bridge.scene = Scene(container)
                }
                val progress = {
                    report(PlaybackEngineEvent.Progress(
                        next.currentTime.toMillis().toLong(), next.totalDuration.toMillis().toLong(),
                    ))
                }
                next.setOnReady { progress() }
                next.currentTimeProperty().addListener { _, _, _ -> progress() }
                next.setOnPlaying { report(PlaybackEngineEvent.Status(true, false)) }
                next.setOnPaused { report(PlaybackEngineEvent.Status(false, false)) }
                next.setOnStalled { report(PlaybackEngineEvent.Status(false, true)) }
                next.setOnEndOfMedia { report(PlaybackEngineEvent.Ended) }
                next.setOnError { report(PlaybackEngineEvent.Failure(next.error?.message.orEmpty())) }
                media.setOnError { report(PlaybackEngineEvent.Failure(media.error?.message.orEmpty())) }
            } catch (exception: Exception) {
                report(PlaybackEngineEvent.Failure(exception.message ?: "Unable to load media"))
            }
        }
    }

    override fun play() = Platform.runLater { player?.play() }
    override fun pause() = Platform.runLater { player?.pause() }
    override fun seekTo(positionMs: Long) = Platform.runLater { player?.seek(Duration.millis(positionMs.toDouble())) }

    override fun close() = Platform.runLater { clearPlayer() }

    private fun clearPlayer() {
        player?.dispose()
        player = null
        if (videoSurface != null) bridge.scene = null
    }
}
