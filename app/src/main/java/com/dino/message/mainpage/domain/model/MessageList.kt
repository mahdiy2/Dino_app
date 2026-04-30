package com.dino.message.mainpage.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MessageList(
    val username: String,
    val displayName: String,
    val badge: String? = null,
    val time: String,
    val newMessage: Boolean = false,
    val online: Boolean = false,
    val lastMessageType: String? = null,
    val profileImageUrl: String? = null,
    val profileImageShape: String? = null,
    val profileVideoUrl: String? = null,
) : Parcelable
