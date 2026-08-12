package com.dino.message.profilefeature.domain.repository

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.profilefeature.domain.model.AboutUs
import com.dino.message.profilefeature.domain.model.Profile
import com.dino.message.profilefeature.domain.model.ProfileFollowResult
import com.dino.message.profilefeature.domain.model.ProfileUpdateImage
import com.dino.message.profilefeature.domain.model.ProfileUsersPage
import com.dino.message.profilefeature.domain.model.ReferralSummary
import com.dino.message.threadfeature.domain.model.PostsPage

interface ProfileRepository {
    suspend fun getAboutUs(): Resource<AboutUs>

    suspend fun getMyProfile(): Resource<Profile>

    suspend fun getMyReferralSummary(): Resource<ReferralSummary>

    suspend fun updateMyProfile(
        name: String? = null,
        bio: String? = null,
        profileImageShape: String? = null,
        profileImage: ProfileUpdateImage? = null,
        profileVideo: ProfileUpdateImage? = null,
        removeProfileImage: Boolean = false,
        removeProfileVideo: Boolean = false,
    ): Resource<Profile>

    suspend fun deleteMyAccount(): Resource<ProfileFollowResult>

    suspend fun getProfile(username: String): Resource<Profile>

    suspend fun getUserPosts(username: String, page: Int, limit: Int): Resource<PostsPage>

    suspend fun followUser(username: String): Resource<ProfileFollowResult>

    suspend fun unfollowUser(username: String): Resource<ProfileFollowResult>

    suspend fun removeFollower(username: String): Resource<ProfileFollowResult>

    suspend fun getFollowStatus(username: String): Resource<Boolean>

    suspend fun getFollowers(username: String, page: Int, limit: Int): Resource<ProfileUsersPage>

    suspend fun getFollowing(username: String, page: Int, limit: Int): Resource<ProfileUsersPage>

    suspend fun getBlockedUsers(page: Int, limit: Int): Resource<ProfileUsersPage>

    suspend fun unblockUser(username: String): Resource<ProfileFollowResult>
}
