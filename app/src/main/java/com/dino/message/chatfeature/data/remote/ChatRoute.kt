package com.dino.message.chatfeature.data.remote

import com.dino.message.corefeature.data.remote.BaseRoute

sealed class ChatRoute(url: String) : BaseRoute(url){
    object GetInbox : ChatRoute("api/messages/inbox")
    data class GetPrivateHistory(val chatId: String) : ChatRoute("api/messages/history/private/$chatId")

    object SendMessage : ChatRoute("api/messages/send")

    data class GetReceiverPublicKey(val username: String) : ChatRoute("api/contacts/$username/public-key")

    object UploadAttachment : ChatRoute("api/messages/attachments")

    object GetUnread : ChatRoute("api/notifications/unread")
}
