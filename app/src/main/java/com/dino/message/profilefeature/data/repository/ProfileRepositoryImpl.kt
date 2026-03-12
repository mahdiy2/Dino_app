package com.dino.message.profilefeature.data.repository

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.profilefeature.data.remote.ProfileApiService
import com.dino.message.profilefeature.domain.model.Profile
import com.dino.message.profilefeature.domain.model.ProfileFollowResult
import com.dino.message.profilefeature.domain.model.ProfileUpdateImage
import com.dino.message.profilefeature.domain.model.ProfileUsersPage
import com.dino.message.profilefeature.domain.repository.ProfileRepository
import com.dino.message.threadfeature.domain.model.PostsPage

class ProfileRepositoryImpl(
    private val profileApiService: ProfileApiService
) : ProfileRepository {
    override suspend fun getMyProfile(): Resource<Profile> = profileApiService.getMyProfile()

    override suspend fun updateMyProfile(
        name: String?,
        bio: String?,
        profileImage: ProfileUpdateImage?,
        profileVideo: ProfileUpdateImage?
    ): Resource<Profile> = profileApiService.updateMyProfile(
        name = name,
        bio = bio,
        profileImage = profileImage,
        profileVideo = profileVideo
    )

    override suspend fun getProfile(username: String): Resource<Profile> =
        profileApiService.getProfile(username = username)

    override suspend fun getUserPosts(username: String, page: Int, limit: Int): Resource<PostsPage> =
        profileApiService.getUserPosts(username = username, page = page, limit = limit)

    override suspend fun followUser(username: String): Resource<ProfileFollowResult> =
        profileApiService.followUser(username = username)

    override suspend fun unfollowUser(username: String): Resource<ProfileFollowResult> =
        profileApiService.unfollowUser(username = username)

    override suspend fun getFollowStatus(username: String): Resource<Boolean> =
        profileApiService.getFollowStatus(username = username)

    override suspend fun getFollowers(
        username: String,
        page: Int,
        limit: Int
    ): Resource<ProfileUsersPage> =
        profileApiService.getFollowers(username = username, page = page, limit = limit)

    override suspend fun getFollowing(
        username: String,
        page: Int,
        limit: Int
    ): Resource<ProfileUsersPage> =
        profileApiService.getFollowing(username = username, page = page, limit = limit)
}
