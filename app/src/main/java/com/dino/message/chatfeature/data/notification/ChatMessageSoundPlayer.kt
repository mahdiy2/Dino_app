package com.dino.message.chatfeature.data.notification

import android.content.Context
import android.media.MediaPlayer
import com.dino.message.R

object ChatMessageSoundPlayer {

    fun playSent(context: Context) {
        play(context, R.raw.sent_message)
    }

    fun playReceived(context: Context) {
        play(context, R.raw.received_message)
    }

    private fun play(context: Context, soundResId: Int) {
        runCatching {
            val mediaPlayer = MediaPlayer.create(context.applicationContext, soundResId) ?: return
            mediaPlayer.setOnCompletionListener { player ->
                player.reset()
                player.release()
            }
            mediaPlayer.setOnErrorListener { player, _, _ ->
                player.reset()
                player.release()
                true
            }
            mediaPlayer.start()
        }
    }
}
