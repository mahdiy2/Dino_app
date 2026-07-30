package com.dino.message.profilefeature.data.remote

import android.content.Context
import android.provider.OpenableColumns
import com.dino.message.corefeature.data.remote.HaveRefreshTokenAPI
import com.dino.message.corefeature.data.remote.HaveRefreshTokenAPIImpl
import com.dino.message.corefeature.data.remote.util.HttpAPIUtil
import com.dino.message.corefeature.data.remote.util.appendTokenToHeader
import com.dino.message.corefeature.data.remote.util.ensureFreshAccessTokenIfNearExpiry
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.data.upload.MediaFileResolver
import com.dino.message.corefeature.domain.model.asStringResourceContent
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.profilefeature.data.remote.dto.AboutUsResponse
import com.dino.message.profilefeature.data.remote.dto.FollowStatusResponse
import com.dino.message.profilefeature.data.remote.dto.GetProfilePostsResponse
import com.dino.message.profilefeature.data.remote.dto.ProfileFollowResponse
import com.dino.message.profilefeature.data.remote.dto.ProfileResponse
import com.dino.message.profilefeature.data.remote.dto.ProfileUsersPageResponse
import com.dino.message.profilefeature.data.remote.dto.ReferralSummaryResponse
import com.dino.message.profilefeature.data.remote.dto.UpdateMyProfileRequest
import com.dino.message.profilefeature.domain.model.Profile
import com.dino.message.profilefeature.domain.model.AboutUs
import com.dino.message.profilefeature.domain.model.ProfileFollowResult
import com.dino.message.profilefeature.domain.model.ProfileUpdateImage
import com.dino.message.profilefeature.domain.model.ProfileUsersPage
import com.dino.message.profilefeature.domain.model.ReferralSummary
import com.dino.message.threadfeature.domain.model.PostsPage
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
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

class ProfileApiServiceImpl constructor(
    private val httpClient: HttpClient,
    private val sPrefManager: SPrefManager,
    private val httpAPIUtil: HttpAPIUtil,
    private val appContext: Context,
) : ProfileApiService, HaveRefreshTokenAPI by HaveRefreshTokenAPIImpl(httpClient, sPrefManager) {

    companion object {
        private const val DELETE_ACCOUNT_REQUEST_TIMEOUT_MS = 120_000L
        private const val PROFILE_MEDIA_UPLOAD_TIMEOUT_MS = 120_000L
    }

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

    override suspend fun getMyReferralSummary(): Resource<ReferralSummary> =
        httpAPIUtil.callAPIWithErrorHandling<ReferralSummary, ReferralSummaryResponse>(
            callAPI = {
                httpClient.get(ProfileRoute.GetMyReferralSummary.url) {
                    appendTokenToHeader(sPrefManager.getToken())
                    contentType(ContentType.Application.Json)
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun updateMyProfile(
        name: String?,
        bio: String?,
        profileImageShape: String?,
        profileImage: ProfileUpdateImage?,
        profileVideo: ProfileUpdateImage?,
        removeProfileImage: Boolean,
        removeProfileVideo: Boolean,
    ): Resource<Profile> {
        val sanitizedName = name?.trim()
        if (sanitizedName != null && sanitizedName.isEmpty()) {
            return Resource.Error(message = "Name must be a non-empty string".asStringResourceContent())
        }
        if (removeProfileImage && profileImage != null) {
            return Resource.Error(message = "Cannot upload and remove profile image in the same request".asStringResourceContent())
        }
        if (removeProfileVideo && profileVideo != null) {
            return Resource.Error(message = "Cannot upload and remove profile video in the same request".asStringResourceContent())
        }
        val sanitizedBio = bio?.trim()
        val sanitizedProfileImageShape = profileImageShape?.trim()?.lowercase()

        if (sanitizedName == null &&
            sanitizedBio == null &&
            sanitizedProfileImageShape == null &&
            profileImage == null &&
            profileVideo == null &&
            !removeProfileImage &&
            !removeProfileVideo
        ) {
            return Resource.Error(message = "At least one field is required".asStringResourceContent())
        }

        return httpAPIUtil.callAPIWithErrorHandling<Profile, ProfileResponse>(
            callAPI = {
                val hasMedia = profileImage != null || profileVideo != null
                val hasRemoveDirectives = removeProfileImage || removeProfileVideo
                if (hasMedia) {
                    ensureFreshAccessTokenIfNearExpiry(
                        sPrefManager = sPrefManager,
                        refreshAccessToken = this@ProfileApiServiceImpl::getToken,
                        logTag = "ProfileApiServiceImpl"
                    )
                }
                httpClient.put(ProfileRoute.UpdateMyProfile.url) {
                    appendTokenToHeader(sPrefManager.getToken())
                    if (!hasMedia) {
                        contentType(ContentType.Application.Json)
                        setBody(
                            UpdateMyProfileRequest(
                                name = sanitizedName,
                                bio = sanitizedBio,
                                profile_image_shape = sanitizedProfileImageShape,
                                remove_profile_image = removeProfileImage.takeIf { it },
                                remove_profile_video = removeProfileVideo.takeIf { it },
                            )
                        )
                    } else {
                        timeout {
                            requestTimeoutMillis = PROFILE_MEDIA_UPLOAD_TIMEOUT_MS
                            socketTimeoutMillis = PROFILE_MEDIA_UPLOAD_TIMEOUT_MS
                        }
                        setBody(
                            MultiPartFormDataContent(
                                formData {
                                    sanitizedName?.let { append("name", it) }
                                    sanitizedBio?.let { append("bio", it) }
                                    sanitizedProfileImageShape?.let { append("profile_image_shape", it) }
                                    if (hasRemoveDirectives) {
                                        if (removeProfileImage) append("remove_profile_image", "true")
                                        if (removeProfileVideo) append("remove_profile_video", "true")
                                    }
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
        return MediaFileResolver.resolveFileSize(appContext.contentResolver, media.uri)
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
                    timeout {
                        requestTimeoutMillis = DELETE_ACCOUNT_REQUEST_TIMEOUT_MS
                        socketTimeoutMillis = DELETE_ACCOUNT_REQUEST_TIMEOUT_MS
                    }
                }
            },
            callRefreshTokenAPI = this::getToken,
            maxAttempts = 1,
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

    override suspend fun removeFollower(username: String): Resource<ProfileFollowResult> =
        httpAPIUtil.callAPIWithErrorHandling<ProfileFollowResult, ProfileFollowResponse>(
            callAPI = {
                httpClient.delete(ProfileRoute.RemoveFollower(username).url) {
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
