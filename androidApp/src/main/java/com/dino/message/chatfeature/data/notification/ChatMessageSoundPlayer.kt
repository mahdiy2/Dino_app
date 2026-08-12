package com.dino.message.chatfeature.data.notification

import android.content.Context
import android.media.AudioManager
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
        if (!canPlaySound(context)) return
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

    private fun canPlaySound(context: Context): Boolean {
        val audioManager = context.applicationContext
            .getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return true
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            return false
        }
        return audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION) > 0
    }
}
