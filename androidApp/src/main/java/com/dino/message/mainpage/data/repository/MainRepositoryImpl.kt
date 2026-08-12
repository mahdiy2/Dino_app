package com.dino.message.mainpage.data.repository

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.mainpage.data.remote.MainApiService
import com.dino.message.mainpage.data.remote.dto.toDomain
import com.dino.message.mainpage.domain.model.AddContact
import com.dino.message.mainpage.domain.model.DetailedContact
import com.dino.message.mainpage.domain.model.PaginatedContacts
import com.dino.message.mainpage.domain.repository.MainRepository

class MainRepositoryImpl(
    private val mainApiService: MainApiService

) : MainRepository {


    override suspend fun addContact(contact: String): Resource<AddContact> =
        mainApiService.addContact(contact)

    override suspend fun getContacts(username: String): Resource<List<String>> =
        mainApiService.getContacts(username)

    override suspend fun getDetailedContacts(page: Int, limit: Int): Resource<PaginatedContacts> =
        when (val result = mainApiService.getDetailedContacts(page, limit)) {
            is Resource.Success -> Resource.Success(result.data.toDomain())
            is Resource.Error -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading()
        }

    override suspend fun getContactsDelta(usernames: List<String>): Resource<List<DetailedContact>> =
        when (val result = mainApiService.getContactsDelta(usernames)) {
            is Resource.Success -> Resource.Success(result.data.map { it.toDomain() })
            is Resource.Error -> Resource.Error(result.message)
            is Resource.Loading -> Resource.Loading()
        }
}
