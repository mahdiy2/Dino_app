package com.dino.message.searchfeature.domain.repository

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.searchfeature.domain.model.SearchAllResult
import com.dino.message.searchfeature.domain.model.SearchPostsResult
import com.dino.message.searchfeature.domain.model.SearchUsersResult

interface SearchRepository {
    suspend fun searchUsers(query: String, page: Int, limit: Int): Resource<SearchUsersResult>
    suspend fun searchPosts(query: String, page: Int, limit: Int): Resource<SearchPostsResult>
    suspend fun searchAll(query: String, page: Int, limit: Int): Resource<SearchAllResult>
}
