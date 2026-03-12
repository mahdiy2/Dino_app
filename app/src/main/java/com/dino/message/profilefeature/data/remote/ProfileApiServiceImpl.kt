package com.dino.message.profilefeature.data.remote

import com.dino.message.corefeature.data.remote.HaveRefreshTokenAPI
import com.dino.message.corefeature.data.remote.HaveRefreshTokenAPIImpl
import com.dino.message.corefeature.data.remote.util.HttpAPIUtil
import com.dino.message.corefeature.data.remote.util.appendTokenToHeader
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.domain.model.asStringResourceContent
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.profilefeature.data.remote.dto.FollowStatusResponse
import com.dino.message.profilefeature.data.remote.dto.GetProfilePostsResponse
import com.dino.message.profilefeature.data.remote.dto.ProfileFollowResponse
import com.dino.message.profilefeature.data.remote.dto.ProfileResponse
import com.dino.message.profilefeature.data.remote.dto.ProfileUsersPageResponse
import com.dino.message.profilefeature.data.remote.dto.UpdateMyProfileRequest
import com.dino.message.profilefeature.domain.model.Profile
import com.dino.message.profilefeature.domain.model.ProfileFollowResult
import com.dino.message.profilefeature.domain.model.ProfileUpdateImage
import com.dino.message.profilefeature.domain.model.ProfileUsersPage
import com.dino.message.threadfeature.domain.model.PostsPage
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import javax.inject.Inject

class ProfileApiServiceImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val sPrefManager: SPrefManager,
    private val httpAPIUtil: HttpAPIUtil
) : ProfileApiService, HaveRefreshTokenAPI by HaveRefreshTokenAPIImpl(httpClient, sPrefManager) {

    override suspend fun getMyProfile(): Resource<Profile> =
        httpAPIUtil.callAPIWithErrorHandling<Profile, ProfileResponse>(
            callAPI = {
                httpClient.get(ProfileRoute.GetMyProfile.url) {
                    appendTokenToHeader(sPrefManager.getToken())
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun updateMyProfile(
        name: String?,
        bio: String?,
        profileImage: ProfileUpdateImage?,
        profileVideo: ProfileUpdateImage?
    ): Resource<Profile> {
        val sanitizedName = name?.trim()
        if (sanitizedName != null && sanitizedName.isEmpty()) {
            return Resource.Error(message = "Name must be a non-empty string".asStringResourceContent())
        }
        val sanitizedBio = bio?.trim()

        if (sanitizedName == null && sanitizedBio == null && profileImage == null && profileVideo == null) {
            return Resource.Error(message = "At least one field is required".asStringResourceContent())
        }

        return httpAPIUtil.callAPIWithErrorHandling<Profile, ProfileResponse>(
            callAPI = {
                httpClient.put(ProfileRoute.UpdateMyProfile.url) {
                    appendTokenToHeader(sPrefManager.getToken())
                    if (profileImage == null && profileVideo == null) {
                        contentType(ContentType.Application.Json)
                        setBody(
                            UpdateMyProfileRequest(
                                name = sanitizedName,
                                bio = sanitizedBio
                            )
                        )
                    } else {
                        setBody(
                            MultiPartFormDataContent(
                                formData {
                                    sanitizedName?.let { append("name", it) }
                                    sanitizedBio?.let { append("bio", it) }
                                    profileImage?.let {
                                        append(
                                            key = "profile_image",
                                            value = it.bytes,
                                            headers = Headers.build {
                                                append(
                                                    HttpHeaders.ContentDisposition,
                                                    "form-data; name=\"profile_image\"; filename=\"${it.fileName}\""
                                                )
                                                append(HttpHeaders.ContentType, it.mimeType)
                                            }
                                        )
                                    }
                                    profileVideo?.let {
                                        append(
                                            key = "profile_video",
                                            value = it.bytes,
                                            headers = Headers.build {
                                                append(
                                                    HttpHeaders.ContentDisposition,
                                                    "form-data; name=\"profile_video\"; filename=\"${it.fileName}\""
                                                )
                                                append(HttpHeaders.ContentType, it.mimeType)
                                            }
                                        )
                                    }
                                }
                            )
                        )
                    }
                }
            },
            callRefreshTokenAPI = this::getToken
        )
    }

    override suspend fun getProfile(username: String): Resource<Profile> =
        httpAPIUtil.callAPIWithErrorHandling<Profile, ProfileResponse>(
            callAPI = {
                httpClient.get(ProfileRoute.GetProfile(username).url)
            },
            callRefreshTokenAPI = this::getToken,
            needToUpdateToken = false
        )

    override suspend fun getUserPosts(
        username: String,
        page: Int,
        limit: Int
    ): Resource<PostsPage> =
        httpAPIUtil.callAPIWithErrorHandling<PostsPage, GetProfilePostsResponse>(
            callAPI = {
                httpClient.get(
                    ProfileRoute.GetUserPosts(
                        username = username,
                        page = page,
                        limit = limit
                    ).url
                ) {
                    contentType(ContentType.Application.Json)
                }
            },
            callRefreshTokenAPI = this::getToken,
            needToUpdateToken = false
        )

    override suspend fun followUser(username: String): Resource<ProfileFollowResult> =
        httpAPIUtil.callAPIWithErrorHandling<ProfileFollowResult, ProfileFollowResponse>(
            callAPI = {
                httpClient.post(ProfileRoute.Follow(username).url) {
                    appendTokenToHeader(sPrefManager.getToken())
                    contentType(ContentType.Application.Json)
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun unfollowUser(username: String): Resource<ProfileFollowResult> =
        httpAPIUtil.callAPIWithErrorHandling<ProfileFollowResult, ProfileFollowResponse>(
            callAPI = {
                httpClient.delete(ProfileRoute.Unfollow(username).url) {
                    appendTokenToHeader(sPrefManager.getToken())
                    contentType(ContentType.Application.Json)
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun getFollowStatus(username: String): Resource<Boolean> =
        httpAPIUtil.callAPIWithErrorHandling<Boolean, FollowStatusResponse>(
            callAPI = {
                httpClient.get(ProfileRoute.FollowStatus(username).url) {
                    appendTokenToHeader(sPrefManager.getToken())
                    contentType(ContentType.Application.Json)
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun getFollowers(
        username: String,
        page: Int,
        limit: Int
    ): Resource<ProfileUsersPage> =
        httpAPIUtil.callAPIWithErrorHandling<ProfileUsersPage, ProfileUsersPageResponse>(
            callAPI = {
                httpClient.get(
                    ProfileRoute.GetFollowers(
                        username = username,
                        page = page,
                        limit = limit
                    ).url
                ) {
                    contentType(ContentType.Application.Json)
                }
            },
            callRefreshTokenAPI = this::getToken,
            needToUpdateToken = false
        )

    override suspend fun getFollowing(
        username: String,
        page: Int,
        limit: Int
    ): Resource<ProfileUsersPage> =
        httpAPIUtil.callAPIWithErrorHandling<ProfileUsersPage, ProfileUsersPageResponse>(
            callAPI = {
                httpClient.get(
                    ProfileRoute.GetFollowing(
                        username = username,
                        page = page,
                        limit = limit
                    ).url
                ) {
                    contentType(ContentType.Application.Json)
                }
            },
            callRefreshTokenAPI = this::getToken,
            needToUpdateToken = false
        )
}
