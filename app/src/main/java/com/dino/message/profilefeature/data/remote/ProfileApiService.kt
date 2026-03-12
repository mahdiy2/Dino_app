package com.dino.message.profilefeature.data.remote

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.profilefeature.domain.model.Profile
import com.dino.message.profilefeature.domain.model.ProfileFollowResult
import com.dino.message.profilefeature.domain.model.ProfileUpdateImage
import com.dino.message.profilefeature.domain.model.ProfileUsersPage
import com.dino.message.threadfeature.domain.model.PostsPage

interface ProfileApiService {
    suspend fun getMyProfile(): Resource<Profile>

    suspend fun updateMyProfile(
        name: String? = null,
        bio: String? = null,
        profileImage: ProfileUpdateImage? = null,
        profileVideo: ProfileUpdateImage? = null
    ): Resource<Profile>

    suspend fun getProfile(username: String): Resource<Profile>

    suspend fun getUserPosts(username: String, page: Int, limit: Int): Resource<PostsPage>

    suspend fun followUser(username: String): Resource<ProfileFollowResult>

    suspend fun unfollowUser(username: String): Resource<ProfileFollowResult>

    suspend fun getFollowStatus(username: String): Resource<Boolean>

    suspend fun getFollowers(username: String, page: Int, limit: Int): Resource<ProfileUsersPage>

    suspend fun getFollowing(username: String, page: Int, limit: Int): Resource<ProfileUsersPage>
}
