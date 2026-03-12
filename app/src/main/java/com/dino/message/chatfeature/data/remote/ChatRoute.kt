package com.dino.message.chatfeature.data.remote

import com.dino.message.corefeature.data.remote.BaseRoute

sealed class ChatRoute(url: String) : BaseRoute(url){
    object GetInbox : ChatRoute("api/messages/inbox")

    object SendMessage : ChatRoute("api/messages/send")

    data class GetReceiverPublicKey(val username: String) : ChatRoute("api/contacts/$username/public-key")
}