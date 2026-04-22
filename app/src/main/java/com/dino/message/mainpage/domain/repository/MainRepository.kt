package com.dino.message.mainpage.domain.repository

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.mainpage.data.remote.dto.DetailedContact
import com.dino.message.mainpage.domain.model.AddContact
import com.dino.message.mainpage.data.remote.dto.PaginatedContacts

interface MainRepository {

    suspend fun addContact(contact: String): Resource<AddContact>

    suspend fun getContacts(username: String): Resource<List<String>>

    suspend fun getDetailedContacts(page: Int = 1, limit: Int = 20): Resource<PaginatedContacts>

    suspend fun getContactsDelta(usernames: List<String>): Resource<List<DetailedContact>>
}
