package com.dino.message.searchfeature.data.remote

import com.dino.message.corefeature.data.remote.HaveRefreshTokenAPI
import com.dino.message.corefeature.data.remote.HaveRefreshTokenAPIImpl
import com.dino.message.corefeature.data.remote.util.HttpAPIUtil
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.searchfeature.data.remote.dto.SearchAllResponse
import com.dino.message.searchfeature.data.remote.dto.SearchPostsResponse
import com.dino.message.searchfeature.data.remote.dto.SearchUsersResponse
import com.dino.message.searchfeature.domain.model.SearchAllResult
import com.dino.message.searchfeature.domain.model.SearchPostsResult
import com.dino.message.searchfeature.domain.model.SearchUsersResult
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.contentType
import javax.inject.Inject

class SearchApiServiceImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val sPrefManager: SPrefManager,
    private val httpAPIUtil: HttpAPIUtil
) : SearchApiService, HaveRefreshTokenAPI by HaveRefreshTokenAPIImpl(httpClient, sPrefManager) {

    override suspend fun searchUsers(
        query: String,
        page: Int,
        limit: Int
    ): Resource<SearchUsersResult> =
        httpAPIUtil.callAPIWithErrorHandling<SearchUsersResult, SearchUsersResponse>(
            callAPI = {
                httpClient.get(SearchRoute.SearchUsers(query = query, page = page, limit = limit).url) {
                    contentType(ContentType.Application.Json)
                }
            },
            callRefreshTokenAPI = this::getToken,
            needToUpdateToken = false
        )

    override suspend fun searchPosts(
        query: String,
        page: Int,
        limit: Int
    ): Resource<SearchPostsResult> =
        httpAPIUtil.callAPIWithErrorHandling<SearchPostsResult, SearchPostsResponse>(
            callAPI = {
                httpClient.get(SearchRoute.SearchPosts(query = query, page = page, limit = limit).url) {
                    contentType(ContentType.Application.Json)
                }
            },
            callRefreshTokenAPI = this::getToken,
            needToUpdateToken = false
        )

    override suspend fun searchAll(
        query: String,
        page: Int,
        limit: Int
    ): Resource<SearchAllResult> =
        httpAPIUtil.callAPIWithErrorHandling<SearchAllResult, SearchAllResponse>(
            callAPI = {
                httpClient.get(SearchRoute.SearchAll(query = query, page = page, limit = limit).url) {
                    contentType(ContentType.Application.Json)
                }
            },
            callRefreshTokenAPI = this::getToken,
            needToUpdateToken = false
        )
}
