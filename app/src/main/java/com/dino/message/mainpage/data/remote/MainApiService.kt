package com.dino.message.mainpage.data.remote

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.mainpage.data.remote.dto.DetailedContact
import com.dino.message.mainpage.domain.model.AddContact
import com.dino.message.mainpage.data.remote.dto.PaginatedContacts

interface MainApiService {

    suspend fun addContact(
        contact: String,
    ): Resource<AddContact>

    suspend fun getContacts(
        username: String
    ): Resource<List<String>>

    suspend fun getDetailedContacts(
        page: Int = 1,
        limit: Int = 20
    ): Resource<PaginatedContacts>

    suspend fun getContactsDelta(
        usernames: List<String>
    ): Resource<List<DetailedContact>>
}
