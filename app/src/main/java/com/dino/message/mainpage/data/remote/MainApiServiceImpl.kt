package com.dino.message.mainpage.data.remote

import com.dino.message.corefeature.data.remote.HaveRefreshTokenAPI
import com.dino.message.corefeature.data.remote.HaveRefreshTokenAPIImpl
import com.dino.message.corefeature.data.remote.util.HttpAPIUtil
import com.dino.message.corefeature.data.remote.util.appendTokenToHeader
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.mainpage.data.remote.dto.AddContactRequest
import com.dino.message.mainpage.data.remote.dto.AddContactResponse
import com.dino.message.mainpage.data.remote.dto.ContactsDeltaResponse
import com.dino.message.mainpage.data.remote.dto.DetailedContact
import com.dino.message.mainpage.data.remote.dto.DetailedContactResponse
import com.dino.message.mainpage.data.remote.dto.GetContactsResponse
import com.dino.message.mainpage.domain.model.AddContact
import com.dino.message.mainpage.data.remote.dto.PaginatedContacts
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject

class MainApiServiceImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val sPrefManager: SPrefManager,
    private val httpAPIUtil: HttpAPIUtil
) : MainApiService, HaveRefreshTokenAPI by HaveRefreshTokenAPIImpl(httpClient, sPrefManager) {



    override suspend fun addContact(
        contact: String
    ): Resource<AddContact> =
        httpAPIUtil.callAPIWithErrorHandling<AddContact, AddContactResponse>(
            callAPI = {
                httpClient.post(MainRoute.AddContact.url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                    setBody(
                        AddContactRequest(
                            contact
                        )
                    )
                }
            },
            callRefreshTokenAPI = this::getToken

        )

    override suspend fun getContacts(username: String): Resource<List<String>> =
        httpAPIUtil.callAPIWithErrorHandling<List<String>, GetContactsResponse>(
            callAPI = {
                httpClient.get(MainRoute.GetContacts.url) {
                    appendTokenToHeader(sPrefManager.getToken())
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun getDetailedContacts(page: Int, limit: Int): Resource<PaginatedContacts> =
        httpAPIUtil.callAPIWithErrorHandling<PaginatedContacts, DetailedContactResponse>(
            callAPI = {
                httpClient.get(MainRoute.GetDetailedContacts.url) {
                    appendTokenToHeader(sPrefManager.getToken())
                    parameter("detailed", "true")
                    parameter("page", page)
                    parameter("limit", limit)
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun getContactsDelta(usernames: List<String>): Resource<List<DetailedContact>> {
        val normalized = usernames
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        if (normalized.isEmpty()) {
            return Resource.Success(emptyList())
        }

        return httpAPIUtil.callAPIWithErrorHandling<List<DetailedContact>, ContactsDeltaResponse>(
            callAPI = {
                httpClient.get(MainRoute.GetContactsDelta.url) {
                    appendTokenToHeader(sPrefManager.getToken())
                    parameter("users", normalized.joinToString(","))
                }
            },
            callRefreshTokenAPI = this::getToken
        )
    }
}
