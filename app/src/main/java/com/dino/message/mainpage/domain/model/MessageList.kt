package com.dino.message.mainpage.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MessageList(
    val username: String,
    val displayName: String,
    val time: String,
    val newMessage: Boolean = false,
    val online: Boolean = false,
    val lastMessageType: String? = null,
    val profileImageUrl: String? = null,
    val profileVideoUrl: String? = null,
) : Parcelable
