package com.dino.message.searchfeature.data.repository

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.searchfeature.data.remote.SearchApiService
import com.dino.message.searchfeature.domain.model.SearchAllResult
import com.dino.message.searchfeature.domain.model.SearchPostsResult
import com.dino.message.searchfeature.domain.model.SearchUsersResult
import com.dino.message.searchfeature.domain.repository.SearchRepository

class SearchRepositoryImpl(
    private val searchApiService: SearchApiService
) : SearchRepository {

    override suspend fun searchUsers(
        query: String,
        page: Int,
        limit: Int
    ): Resource<SearchUsersResult> =
        searchApiService.searchUsers(query = query, page = page, limit = limit)

    override suspend fun searchPosts(
        query: String,
        page: Int,
        limit: Int
    ): Resource<SearchPostsResult> =
        searchApiService.searchPosts(query = query, page = page, limit = limit)

    override suspend fun searchAll(
        query: String,
        page: Int,
        limit: Int
    ): Resource<SearchAllResult> =
        searchApiService.searchAll(query = query, page = page, limit = limit)
}
