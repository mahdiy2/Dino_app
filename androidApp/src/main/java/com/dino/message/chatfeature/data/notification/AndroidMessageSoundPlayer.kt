package com.dino.message.chatfeature.data.notification

import android.content.Context
import com.dino.message.chatfeature.domain.audio.MessageSoundPlayer

class AndroidMessageSoundPlayer(
    private val context: Context,
) : MessageSoundPlayer {
    override fun playSent() {
        ChatMessageSoundPlayer.playSent(context)
    }

    override fun playReceived() {
        ChatMessageSoundPlayer.playReceived(context)
    }
}
