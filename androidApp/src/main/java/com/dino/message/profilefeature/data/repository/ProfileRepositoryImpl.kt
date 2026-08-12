package com.dino.message.profilefeature.data.repository

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.profilefeature.data.remote.ProfileApiService
import com.dino.message.profilefeature.domain.model.AboutUs
import com.dino.message.profilefeature.domain.model.Profile
import com.dino.message.profilefeature.domain.model.ProfileFollowResult
import com.dino.message.profilefeature.domain.model.ProfileUpdateImage
import com.dino.message.profilefeature.domain.model.ProfileUsersPage
import com.dino.message.profilefeature.domain.model.ReferralSummary
import com.dino.message.profilefeature.domain.repository.ProfileRepository
import com.dino.message.threadfeature.domain.model.PostsPage

class ProfileRepositoryImpl(
    private val profileApiService: ProfileApiService
) : ProfileRepository {
    override suspend fun getAboutUs(): Resource<AboutUs> = profileApiService.getAboutUs()

    override suspend fun getMyProfile(): Resource<Profile> = profileApiService.getMyProfile()

    override suspend fun getMyReferralSummary(): Resource<ReferralSummary> =
        profileApiService.getMyReferralSummary()

    override suspend fun updateMyProfile(
        name: String?,
        bio: String?,
        profileImageShape: String?,
        profileImage: ProfileUpdateImage?,
        profileVideo: ProfileUpdateImage?,
        removeProfileImage: Boolean,
        removeProfileVideo: Boolean,
    ): Resource<Profile> = profileApiService.updateMyProfile(
        name = name,
        bio = bio,
        profileImageShape = profileImageShape,
        profileImage = profileImage,
        profileVideo = profileVideo,
        removeProfileImage = removeProfileImage,
        removeProfileVideo = removeProfileVideo,
    )

    override suspend fun deleteMyAccount(): Resource<ProfileFollowResult> =
        profileApiService.deleteMyAccount()

    override suspend fun getProfile(username: String): Resource<Profile> =
        profileApiService.getProfile(username = username)

    override suspend fun getUserPosts(username: String, page: Int, limit: Int): Resource<PostsPage> =
        profileApiService.getUserPosts(username = username, page = page, limit = limit)

    override suspend fun followUser(username: String): Resource<ProfileFollowResult> =
        profileApiService.followUser(username = username)

    override suspend fun unfollowUser(username: String): Resource<ProfileFollowResult> =
        profileApiService.unfollowUser(username = username)

    override suspend fun removeFollower(username: String): Resource<ProfileFollowResult> =
        profileApiService.removeFollower(username = username)

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

    override suspend fun getBlockedUsers(page: Int, limit: Int): Resource<ProfileUsersPage> =
        profileApiService.getBlockedUsers(page = page, limit = limit)

    override suspend fun unblockUser(username: String): Resource<ProfileFollowResult> =
        profileApiService.unblockUser(username = username)
}
