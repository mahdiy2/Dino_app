package com.dino.message.mainpage.data.remote

import com.dino.message.corefeature.data.remote.BaseRoute

sealed class MainRoute(url: String) : BaseRoute(url) {

    object AddContact : MainRoute("api/auth/contacts")

    object GetContacts : MainRoute("api/contacts")

    object GetDetailedContacts : MainRoute("api/contacts")

    object GetContactsDelta : MainRoute("api/contacts/delta")
}
