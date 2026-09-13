package com.dino.message.searchfeature.data.remote

import com.dino.message.corefeature.data.remote.BaseRoute

sealed class SearchRoute(url: String) : BaseRoute(url) {
    data class SearchUsers(val query: String, val page: Int, val limit: Int) :
        SearchRoute("api/search/users?q=$query&page=$page&limit=$limit")

    data class SearchPosts(val query: String, val page: Int, val limit: Int) :
        SearchRoute("api/search/posts?q=$query&page=$page&limit=$limit")

    data class SearchAll(val query: String, val page: Int, val limit: Int) :
        SearchRoute("api/search?q=$query&page=$page&limit=$limit")
}
