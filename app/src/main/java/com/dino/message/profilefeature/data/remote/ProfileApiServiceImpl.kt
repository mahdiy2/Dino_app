package com.dino.message.profilefeature.data.remote

import android.content.Context
import android.provider.OpenableColumns
import com.dino.message.corefeature.data.remote.HaveRefreshTokenAPI
import com.dino.message.corefeature.data.remote.HaveRefreshTokenAPIImpl
import com.dino.message.corefeature.data.remote.util.HttpAPIUtil
import com.dino.message.corefeature.data.remote.util.appendTokenToHeader
import com.dino.message.corefeature.data.remote.util.ensureFreshAccessTokenIfNearExpiry
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.domain.model.asStringResourceContent
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.profilefeature.data.remote.dto.AboutUsResponse
import com.dino.message.profilefeature.data.remote.dto.FollowStatusResponse
import com.dino.message.profilefeature.data.remote.dto.GetProfilePostsResponse
import com.dino.message.profilefeature.data.remote.dto.ProfileFollowResponse
import com.dino.message.profilefeature.data.remote.dto.ProfileResponse
import com.dino.message.profilefeature.data.remote.dto.ProfileUsersPageResponse
import com.dino.message.profilefeature.data.remote.dto.UpdateMyProfileRequest
import com.dino.message.profilefeature.domain.model.Profile
import com.dino.message.profilefeature.domain.model.AboutUs
import com.dino.message.profilefeature.domain.model.ProfileFollowResult
import com.dino.message.profilefeature.domain.model.ProfileUpdateImage
import com.dino.message.profilefeature.domain.model.ProfileUsersPage
import com.dino.message.threadfeature.domain.model.PostsPage
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.forms.InputProvider
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
import io.ktor.utils.io.streams.asInput
import java.io.File
import javax.inject.Inject

class ProfileApiServiceImpl @Inject constructor(
    private val httpClient: HttpClient,
    private val sPrefManager: SPrefManager,
    private val httpAPIUtil: HttpAPIUtil,
    @ApplicationContext private val appContext: Context,
) : ProfileApiService, HaveRefreshTokenAPI by HaveRefreshTokenAPIImpl(httpClient, sPrefManager) {

    override suspend fun getAboutUs(): Resource<AboutUs> =
        httpAPIUtil.callAPIWithErrorHandling<AboutUs, AboutUsResponse>(
            callAPI = {
                httpClient.get(ProfileRoute.GetAboutUs.url) {
                    contentType(ContentType.Application.Json)
                }
            },
            callRefreshTokenAPI = this::getToken,
            needToUpdateToken = false
        )

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
        profileImageShape: String?,
        profileImage: ProfileUpdateImage?,
        profileVideo: ProfileUpdateImage?
    ): Resource<Profile> {
        val sanitizedName = name?.trim()
        if (sanitizedName != null && sanitizedName.isEmpty()) {
            return Resource.Error(message = "Name must be a non-empty string".asStringResourceContent())
        }
        val sanitizedBio = bio?.trim()
        val sanitizedProfileImageShape = profileImageShape?.trim()?.lowercase()

        if (sanitizedName == null &&
            sanitizedBio == null &&
            sanitizedProfileImageShape == null &&
            profileImage == null &&
            profileVideo == null
        ) {
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
                                bio = sanitizedBio,
                                profile_image_shape = sanitizedProfileImageShape
                            )
                        )
                    } else {
                        ensureFreshAccessTokenIfNearExpiry(
                            sPrefManager = sPrefManager,
                            refreshAccessToken = this@ProfileApiServiceImpl::getToken,
                            logTag = "ProfileApiServiceImpl"
                        )
                        setBody(
                            MultiPartFormDataContent(
                                formData {
                                    sanitizedName?.let { append("name", it) }
                                    sanitizedBio?.let { append("bio", it) }
                                    sanitizedProfileImageShape?.let { append("profile_image_shape", it) }
                                    profileImage?.let {
                                        append(
                                            key = "profile_image",
                                            value = InputProvider(size = resolveFileSize(it)) {
                                                openInputStream(it).asInput()
                                            },
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
                                            value = InputProvider(size = resolveFileSize(it)) {
                                                openInputStream(it).asInput()
                                            },
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

    private fun resolveFileSize(media: ProfileUpdateImage): Long? {
        media.sizeBytes?.let { if (it >= 0) return it }
        val uri = media.uri
        if (uri.scheme == "file") {
            val path = uri.path ?: return null
            val file = File(path)
            return if (file.exists()) file.length() else null
        }
        val cursor = appContext.contentResolver.query(uri, null, null, null, null)
            ?: return null
        return cursor.use {
            if (!it.moveToFirst()) return@use null
            val index = it.getColumnIndex(OpenableColumns.SIZE)
            if (index >= 0 && !it.isNull(index)) it.getLong(index) else null
        }
    }

    private fun openInputStream(media: ProfileUpdateImage): java.io.InputStream {
        val uri = media.uri
        if (uri.scheme == "file") {
            val path = uri.path ?: error("File path is missing")
            val file = File(path)
            if (!file.exists()) error("File does not exist")
            return file.inputStream()
        }
        return appContext.contentResolver.openInputStream(uri)
            ?: error("Unable to open input stream")
    }

    override suspend fun deleteMyAccount(): Resource<ProfileFollowResult> =
        httpAPIUtil.callAPIWithErrorHandling<ProfileFollowResult, ProfileFollowResponse>(
            callAPI = {
                httpClient.delete(ProfileRoute.DeleteMyProfile.url) {
                    appendTokenToHeader(sPrefManager.getToken())
                    contentType(ContentType.Application.Json)
                }
            },
            callRefreshTokenAPI = this::getToken
        )

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
                    appendTokenToHeader(sPrefManager.getToken())
                }
            },
            callRefreshTokenAPI = this::getToken
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

    override suspend fun getBlockedUsers(
        page: Int,
        limit: Int
    ): Resource<ProfileUsersPage> =
        httpAPIUtil.callAPIWithErrorHandling<ProfileUsersPage, ProfileUsersPageResponse>(
            callAPI = {
                httpClient.get(
                    ProfileRoute.GetBlockedUsers(
                        page = page,
                        limit = limit
                    ).url
                ) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun unblockUser(username: String): Resource<ProfileFollowResult> =
        httpAPIUtil.callAPIWithErrorHandling<ProfileFollowResult, ProfileFollowResponse>(
            callAPI = {
                httpClient.delete(ProfileRoute.UnblockUser(username).url) {
                    appendTokenToHeader(sPrefManager.getToken())
                    contentType(ContentType.Application.Json)
                }
            },
            callRefreshTokenAPI = this::getToken
        )
}
