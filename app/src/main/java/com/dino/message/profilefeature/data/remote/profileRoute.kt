package com.dino.message.profilefeature.data.remote

import com.dino.message.corefeature.data.remote.BaseRoute

sealed class ProfileRoute(url: String) : BaseRoute(url) {
    object GetMyProfile : ProfileRoute("api/profiles/me")

    object UpdateMyProfile : ProfileRoute("api/profiles/me")

    data class GetProfile(val username: String) : ProfileRoute("api/profiles/$username")

    data class GetUserPosts(
        val username: String,
        val page: Int,
        val limit: Int
    ) : ProfileRoute("api/profiles/$username/posts?page=$page&limit=$limit")

    data class Follow(val username: String) : ProfileRoute("api/follows/$username")

    data class Unfollow(val username: String) : ProfileRoute("api/follows/$username")

    data class FollowStatus(val username: String) : ProfileRoute("api/follows/$username/status")

    data class GetFollowers(
        val username: String,
        val page: Int,
        val limit: Int
    ) : ProfileRoute("api/follows/$username/followers?page=$page&limit=$limit")

    data class GetFollowing(
        val username: String,
        val page: Int,
        val limit: Int
    ) : ProfileRoute("api/follows/$username/following?page=$page&limit=$limit")
}
