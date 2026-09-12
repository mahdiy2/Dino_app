package com.dino.message.mainpage.domain.repository

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.mainpage.domain.model.AddContact
import com.dino.message.mainpage.domain.model.DetailedContact
import com.dino.message.mainpage.domain.model.PaginatedContacts

interface MainRepository {

    suspend fun addContact(contact: String): Resource<AddContact>

    suspend fun getContacts(username: String): Resource<List<String>>

    suspend fun getDetailedContacts(page: Int = 1, limit: Int = 20): Resource<PaginatedContacts>

    suspend fun getContactsDelta(usernames: List<String>): Resource<List<DetailedContact>>
}
