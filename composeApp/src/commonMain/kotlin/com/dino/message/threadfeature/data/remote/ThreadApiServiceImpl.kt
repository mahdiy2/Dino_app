package com.dino.message.threadfeature.data.remote

import com.dino.message.corefeature.data.remote.HaveRefreshTokenAPI
import com.dino.message.corefeature.data.remote.HaveRefreshTokenAPIImpl
import com.dino.message.corefeature.data.remote.NetworkIssueReporter
import com.dino.message.corefeature.data.remote.util.CommonUuid
import com.dino.message.corefeature.data.remote.util.HttpAPIUtil
import com.dino.message.corefeature.data.remote.util.appendTokenToHeader
import com.dino.message.corefeature.data.remote.util.ensureFreshAccessTokenIfNearExpiry
import com.dino.message.corefeature.domain.crypto.Base64Codec
import com.dino.message.corefeature.domain.logging.AppLogger
import com.dino.message.corefeature.domain.session.AuthSessionStore
import com.dino.message.corefeature.domain.upload.PlatformMediaMetadataExtractor
import com.dino.message.corefeature.domain.upload.PlatformMediaReference
import com.dino.message.corefeature.data.upload.appendUploadFile
import com.dino.message.corefeature.data.upload.canOpenForUpload
import com.dino.message.corefeature.data.upload.toUploadFilePart
import com.dino.message.corefeature.domain.model.asStringResourceContent
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.corefeature.domain.resource.StringResolver
import com.dino.message.corefeature.domain.system.AppClock
import com.dino.message.threadfeature.data.remote.dto.CreateCommentRequest
import com.dino.message.threadfeature.data.remote.dto.CreateCommentResponse
import com.dino.message.threadfeature.data.remote.dto.CreatePostRequest
import com.dino.message.threadfeature.data.remote.dto.CreatePostResponse
import com.dino.message.threadfeature.data.remote.dto.EditPostRequest
import com.dino.message.threadfeature.data.remote.dto.AddPlaylistTrackRequest
import com.dino.message.threadfeature.data.remote.dto.AddPlaylistTrackResponse
import com.dino.message.threadfeature.data.remote.dto.BlockUserResponse
import com.dino.message.threadfeature.data.remote.dto.DeleteContentResponse
import com.dino.message.threadfeature.data.remote.dto.GetCommentsResponse
import com.dino.message.threadfeature.data.remote.dto.GetFeedResponse
import com.dino.message.threadfeature.data.remote.dto.GetPlaylistTracksResponse
import com.dino.message.threadfeature.data.remote.dto.PlaylistTrackExistsResponse
import com.dino.message.threadfeature.data.remote.dto.GetPostResponse
import com.dino.message.threadfeature.data.remote.dto.GetStoryBundleResponse
import com.dino.message.threadfeature.data.remote.dto.GetStoryFeedResponse
import com.dino.message.threadfeature.data.remote.dto.GetStoryMentionCandidatesResponse
import com.dino.message.threadfeature.data.remote.dto.GetStoryViewersResponse
import com.dino.message.threadfeature.data.remote.dto.StoryLikeRequest
import com.dino.message.threadfeature.data.remote.dto.StoryLikeResponse
import com.dino.message.threadfeature.data.remote.dto.StoryReplyRequest
import com.dino.message.threadfeature.data.remote.dto.StoryReplyResponse
import com.dino.message.threadfeature.data.remote.dto.StoryUploadResponse
import com.dino.message.threadfeature.data.remote.dto.StoryViewRequest
import com.dino.message.threadfeature.data.remote.dto.StoryViewResponse
import com.dino.message.threadfeature.data.remote.dto.VoteRequest
import com.dino.message.threadfeature.data.remote.dto.VoteResponse
import com.dino.message.threadfeature.domain.model.BlockedUserResult
import com.dino.message.threadfeature.domain.model.Comment
import com.dino.message.threadfeature.domain.model.DeleteContentResult
import com.dino.message.threadfeature.domain.model.CreatedComment
import com.dino.message.threadfeature.domain.model.CreatedPost
import com.dino.message.threadfeature.domain.model.CreatePostMedia
import com.dino.message.threadfeature.domain.model.FeedMode
import com.dino.message.threadfeature.domain.model.FeedPage
import com.dino.message.threadfeature.domain.model.Post
import com.dino.message.threadfeature.domain.model.PlaylistPage
import com.dino.message.threadfeature.domain.model.PlaylistTrackMutation
import com.dino.message.threadfeature.domain.model.StoryBundle
import com.dino.message.threadfeature.domain.model.StoryFeed
import com.dino.message.threadfeature.domain.model.StoryLikeResult
import com.dino.message.threadfeature.domain.model.StoryReplyResult
import com.dino.message.threadfeature.domain.model.StoryUploadResult
import com.dino.message.threadfeature.domain.model.StoryViewResult
import com.dino.message.threadfeature.domain.model.StoryViewersPage
import com.dino.message.threadfeature.domain.model.VoteResult
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.plugins.timeout
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ThreadApiServiceImpl constructor(
    private val httpClient: HttpClient,
    private val sPrefManager: AuthSessionStore,
    private val httpAPIUtil: HttpAPIUtil,
    private val networkIssueReporter: NetworkIssueReporter,
    private val stringResolver: StringResolver,
    private val mediaMetadataExtractor: PlatformMediaMetadataExtractor,
    private val appClock: AppClock,
    private val appLogger: AppLogger,
    private val base64Codec: Base64Codec,
) : ThreadApiService, HaveRefreshTokenAPI by HaveRefreshTokenAPIImpl(httpClient, sPrefManager, appLogger) {

    companion object {
        private const val MAX_MEDIA_FILES_PER_POST = 8
        private const val MAX_STORY_VIDEO_DURATION_SECONDS = 30.0
        private const val POST_UPLOAD_REQUEST_TIMEOUT_MS = 120_000L
        private const val POST_UPLOAD_SOCKET_TIMEOUT_MS = 120_000L
        private const val STORY_UPLOAD_REQUEST_TIMEOUT_MS = 120_000L
        private const val STORY_UPLOAD_SOCKET_TIMEOUT_MS = 120_000L
    }

    override suspend fun createPost(
        text: String,
        mediaFiles: List<CreatePostMedia>,
        followersOnly: Boolean,
        quotedPostId: Long?
    ): Resource<CreatedPost> {
        if (mediaFiles.isNotEmpty()) {
            ensureFreshAccessTokenBeforeUpload()
        }

        if (mediaFiles.size > MAX_MEDIA_FILES_PER_POST) {
            return Resource.Error(
                message = "Maximum 8 media files allowed".asStringResourceContent()
            )
        }

        val idempotencyKey = CommonUuid.randomUuidString()
        val result = httpAPIUtil.callAPIWithErrorHandling<CreatedPost, CreatePostResponse>(
            callAPI = {
                httpClient.post(ThreadRoute.CreatePost.url) {
                    appendTokenToHeader(sPrefManager.getToken())
                    header("Idempotency-Key", idempotencyKey)
                    timeout {
                        requestTimeoutMillis = POST_UPLOAD_REQUEST_TIMEOUT_MS
                        socketTimeoutMillis = POST_UPLOAD_SOCKET_TIMEOUT_MS
                    }

                    if (mediaFiles.isEmpty()) {
                        contentType(ContentType.Application.Json)
                        setBody(
                            CreatePostRequest(
                                text = text,
                                followers_only = followersOnly,
                                quoted_post_id = quotedPostId
                            )
                        )
                    } else {
                        setBody(
                            MultiPartFormDataContent(
                                formData {
                                    append("text", text)
                                    append("followers_only", followersOnly.toString())
                                    quotedPostId?.let {
                                        append("quoted_post_id", it.toString())
                                    }
                                    val firstAudioMedia = mediaFiles.firstOrNull {
                                        it.mimeType.startsWith("audio/", ignoreCase = true)
                                    }
                                    firstAudioMedia?.trackTitle
                                        ?.trim()
                                        ?.takeIf { it.isNotBlank() }
                                        ?.let { append("track_title", it) }
                                    firstAudioMedia?.trackArtist
                                        ?.trim()
                                        ?.takeIf { it.isNotBlank() }
                                        ?.let { append("track_artist", it) }
                                    mediaFiles.forEach { mediaFile ->
                                        val fileName = mediaFile.fileName.ifBlank { "media_file" }
                                        appendUploadFile(
                                            mediaFile.file.copy(
                                                handle = mediaFile.file.handle.copy(displayName = fileName)
                                            ).toUploadFilePart(formName = "media[]")
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

        if (result is Resource.Error) {
            val errorMessage = result.message.resolveWith(stringResolver)
            networkIssueReporter.reportNonFatalIssue(
                issueType = "CreatePostRequestFailed",
                message = errorMessage,
                category = "network",
                details = buildString {
                    append("endpoint=")
                    append(ThreadRoute.CreatePost.url)
                    append('\n')
                    append("idempotency_key=")
                    append(idempotencyKey)
                    append('\n')
                    append("followers_only=")
                    append(followersOnly)
                    append('\n')
                    append("quoted_post_id=")
                    append(quotedPostId?.toString() ?: "null")
                    append('\n')
                    append("media_count=")
                    append(mediaFiles.size)
                    append('\n')
                    append("media_mime_types=")
                    append(
                        mediaFiles.joinToString(separator = ",") {
                            it.mimeType.trim().ifBlank { "unknown" }
                        }
                    )
                    append('\n')
                    append("error_message=")
                    append(errorMessage)
                }
            )
        }

        return result
    }

    override suspend fun editPost(
        postId: Long,
        text: String,
        mediaFiles: List<CreatePostMedia>,
        keepMediaIds: List<Long>,
        followersOnly: Boolean?
    ): Resource<DeleteContentResult> {
        if (mediaFiles.isNotEmpty()) {
            ensureFreshAccessTokenBeforeUpload()
        }

        if (mediaFiles.size > MAX_MEDIA_FILES_PER_POST) {
            return Resource.Error(
                message = "Maximum 8 media files allowed".asStringResourceContent()
            )
        }

        val result = httpAPIUtil.callAPIWithErrorHandling<DeleteContentResult, DeleteContentResponse>(
            callAPI = {
                httpClient.put(ThreadRoute.UpdatePost(postId).url) {
                    appendTokenToHeader(sPrefManager.getToken())
                    timeout {
                        requestTimeoutMillis = POST_UPLOAD_REQUEST_TIMEOUT_MS
                        socketTimeoutMillis = POST_UPLOAD_SOCKET_TIMEOUT_MS
                    }

                    if (mediaFiles.isEmpty()) {
                        contentType(ContentType.Application.Json)
                        setBody(
                            EditPostRequest(
                                text = text,
                                keep_media_ids = keepMediaIds,
                                followers_only = followersOnly
                            )
                        )
                    } else {
                        setBody(
                            MultiPartFormDataContent(
                                formData {
                                    append("text", text)
                                    keepMediaIds.forEach { mediaId ->
                                        append("keep_media_ids[]", mediaId.toString())
                                    }
                                    followersOnly?.let {
                                        append("followers_only", it.toString())
                                    }
                                    val firstAudioMedia = mediaFiles.firstOrNull {
                                        it.mimeType.startsWith("audio/", ignoreCase = true)
                                    }
                                    firstAudioMedia?.trackTitle
                                        ?.trim()
                                        ?.takeIf { it.isNotBlank() }
                                        ?.let { append("track_title", it) }
                                    firstAudioMedia?.trackArtist
                                        ?.trim()
                                        ?.takeIf { it.isNotBlank() }
                                        ?.let { append("track_artist", it) }
                                    mediaFiles.forEach { mediaFile ->
                                        val fileName = mediaFile.fileName.ifBlank { "media_file" }
                                        appendUploadFile(
                                            mediaFile.file.copy(
                                                handle = mediaFile.file.handle.copy(displayName = fileName)
                                            ).toUploadFilePart(formName = "media[]")
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

        if (result is Resource.Error) {
            val errorMessage = result.message.resolveWith(stringResolver)
            networkIssueReporter.reportNonFatalIssue(
                issueType = "EditPostRequestFailed",
                message = errorMessage,
                category = "network",
                details = buildString {
                    append("endpoint=")
                    append(ThreadRoute.UpdatePost(postId).url)
                    append('\n')
                    append("post_id=")
                    append(postId)
                    append('\n')
                    append("keep_media_ids=")
                    append(keepMediaIds.joinToString(","))
                    append('\n')
                    append("followers_only=")
                    append(followersOnly?.toString() ?: "null")
                    append('\n')
                    append("media_count=")
                    append(mediaFiles.size)
                    append('\n')
                    append("error_message=")
                    append(errorMessage)
                }
            )
        }

        return result
    }

    override suspend fun getFeed(
        mode: FeedMode,
        cursor: String?,
        limit: Int
    ): Resource<FeedPage> =
        httpAPIUtil.callAPIWithErrorHandling<FeedPage, GetFeedResponse>(
            callAPI = {
                httpClient.get(ThreadRoute.GetFeed.url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                    parameter("mode", mode.apiValue)
                    parameter("limit", limit)
                    if (!cursor.isNullOrBlank()) {
                        parameter("cursor", cursor)
                    }
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun getPost(postId: Long): Resource<Post> =
        httpAPIUtil.callAPIWithErrorHandling<Post, GetPostResponse>(
            callAPI = {
                httpClient.get(ThreadRoute.GetPost(postId).url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun deletePost(postId: Long): Resource<DeleteContentResult> =
        httpAPIUtil.callAPIWithErrorHandling<DeleteContentResult, DeleteContentResponse>(
            callAPI = {
                httpClient.delete(ThreadRoute.DeletePost(postId).url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun getComments(
        postId: Long,
        page: Int,
        pageSize: Int
    ): Resource<List<Comment>> =
        httpAPIUtil.callAPIWithErrorHandling<List<Comment>, GetCommentsResponse>(
            callAPI = {
                httpClient.get(
                    ThreadRoute.GetComments(
                        postId = postId,
                        page = page,
                        pageSize = pageSize
                    ).url
                ) {
                    contentType(ContentType.Application.Json)
                }
            },
            callRefreshTokenAPI = this::getToken,
            needToUpdateToken = false
        )

    override suspend fun createComment(
        postId: Long,
        text: String,
        parentId: Long?
    ): Resource<CreatedComment> =
        httpAPIUtil.callAPIWithErrorHandling<CreatedComment, CreateCommentResponse>(
            callAPI = {
                httpClient.post(ThreadRoute.CreateComment(postId).url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                    setBody(CreateCommentRequest(text = text, parent_id = parentId))
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun deleteComment(commentId: Long): Resource<DeleteContentResult> =
        httpAPIUtil.callAPIWithErrorHandling<DeleteContentResult, DeleteContentResponse>(
            callAPI = {
                httpClient.delete(ThreadRoute.DeleteComment(commentId).url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun vote(
        targetType: String,
        targetId: Long,
        value: Long
    ): Resource<VoteResult> =
        httpAPIUtil.callAPIWithErrorHandling<VoteResult, VoteResponse>(
            callAPI = {
                httpClient.post(ThreadRoute.Vote.url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                    setBody(
                        VoteRequest(
                            target_type = targetType,
                            target_id = targetId,
                            value = value
                        )
                    )
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun blockUser(username: String): Resource<BlockedUserResult> =
        httpAPIUtil.callAPIWithErrorHandling<BlockedUserResult, BlockUserResponse>(
            callAPI = {
                httpClient.post(ThreadRoute.BlockUser(username).url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun addTrackToPlaylist(mediaId: Long): Resource<PlaylistTrackMutation> =
        httpAPIUtil.callAPIWithErrorHandling<PlaylistTrackMutation, AddPlaylistTrackResponse>(
            callAPI = {
                httpClient.post(ThreadRoute.PlaylistTracks.url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                    setBody(AddPlaylistTrackRequest(media_id = mediaId))
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun removeTrackFromPlaylist(trackId: Long): Resource<DeleteContentResult> =
        httpAPIUtil.callAPIWithErrorHandling<DeleteContentResult, DeleteContentResponse>(
            callAPI = {
                httpClient.delete(ThreadRoute.DeletePlaylistTrack(trackId).url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun getPlaylistTracks(
        page: Int,
        limit: Int
    ): Resource<PlaylistPage> =
        httpAPIUtil.callAPIWithErrorHandling<PlaylistPage, GetPlaylistTracksResponse>(
            callAPI = {
                httpClient.get(ThreadRoute.GetPlaylistTracks(page = page, limit = limit).url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun isTrackInPlaylist(trackUrl: String): Resource<Boolean> =
        httpAPIUtil.callAPIWithErrorHandling<Boolean, PlaylistTrackExistsResponse>(
            callAPI = {
                httpClient.get(ThreadRoute.PlaylistTrackExists.url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                    parameter("track_url", trackUrl)
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun uploadStory(
        mediaFile: CreatePostMedia,
        mentionUsernames: List<String>
    ): Resource<StoryUploadResult> {
        ensureFreshAccessTokenBeforeUpload()

        val normalizedMimeType = mediaFile.mimeType.trim().lowercase()
        if (normalizedMimeType.startsWith("video/")) {
            val durationSeconds = resolveVideoDurationSeconds(mediaFile)
            if (durationSeconds != null && durationSeconds > MAX_STORY_VIDEO_DURATION_SECONDS) {
                return Resource.Error(
                    message = "Story videos must be 30 seconds or shorter"
                        .asStringResourceContent()
                )
            }
        }

        val result = httpAPIUtil.callAPIWithErrorHandling<StoryUploadResult, StoryUploadResponse>(
            callAPI = {
                httpClient.post(ThreadRoute.UploadStory.url) {
                    appendTokenToHeader(sPrefManager.getToken())
                    timeout {
                        requestTimeoutMillis = STORY_UPLOAD_REQUEST_TIMEOUT_MS
                        socketTimeoutMillis = STORY_UPLOAD_SOCKET_TIMEOUT_MS
                    }
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                val fileName = mediaFile.fileName.ifBlank { "story_media" }
                                appendUploadFile(
                                    mediaFile.file.copy(
                                        handle = mediaFile.file.handle.copy(displayName = fileName)
                                    ).toUploadFilePart(formName = "file")
                                )
                                mentionUsernames
                                    .asSequence()
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }
                                    .forEach { mention ->
                                        append("mentions[]", mention)
                                    }
                            },
                            boundary = createMultipartBoundary()
                        )
                    )
                }
            },
            callRefreshTokenAPI = this::getToken
        )

        if (result is Resource.Error) {
            val errorMessage = result.message.resolveWith(stringResolver)
            networkIssueReporter.reportNonFatalIssue(
                issueType = "UploadStoryRequestFailed",
                message = errorMessage,
                category = "network",
                details = buildString {
                    append("endpoint=")
                    append(ThreadRoute.UploadStory.url)
                    append('\n')
                    append("media_mime_type=")
                    append(mediaFile.mimeType.trim().ifBlank { "unknown" })
                    append('\n')
                    append("media_file_name=")
                    append(mediaFile.fileName.ifBlank { "story_media" })
                    append('\n')
                    append("media_size_bytes=")
                    append(mediaFile.sizeBytes?.toString() ?: "null")
                    append('\n')
                    append("mentions_count=")
                    append(mentionUsernames.size)
                    append('\n')
                    append("error_message=")
                    append(errorMessage)
                }
            )
        }

        return result
    }

    override suspend fun getStoryFeed(): Resource<StoryFeed> =
        httpAPIUtil.callAPIWithErrorHandling<StoryFeed, GetStoryFeedResponse>(
            callAPI = {
                httpClient.get(ThreadRoute.StoryFeed.url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun getStoryBundle(storyId: Long): Resource<StoryBundle> =
        httpAPIUtil.callAPIWithErrorHandling<StoryBundle, GetStoryBundleResponse>(
            callAPI = {
                httpClient.get(ThreadRoute.StoryById(storyId).url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun deleteStory(storyId: Long): Resource<DeleteContentResult> =
        httpAPIUtil.callAPIWithErrorHandling<DeleteContentResult, DeleteContentResponse>(
            callAPI = {
                httpClient.delete(ThreadRoute.DeleteStory(storyId).url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun recordStoryView(storyId: Long): Resource<StoryViewResult> =
        httpAPIUtil.callAPIWithErrorHandling<StoryViewResult, StoryViewResponse>(
            callAPI = {
                httpClient.post(ThreadRoute.StoryView.url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                    setBody(StoryViewRequest(story_id = storyId))
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun setStoryLike(storyId: Long, liked: Boolean): Resource<StoryLikeResult> =
        httpAPIUtil.callAPIWithErrorHandling<StoryLikeResult, StoryLikeResponse>(
            callAPI = {
                httpClient.post(ThreadRoute.StoryLike.url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                    setBody(
                        StoryLikeRequest(
                            story_id = storyId,
                            liked = liked
                        )
                    )
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun getStoryViewers(
        storyId: Long,
        page: Int,
        limit: Int
    ): Resource<StoryViewersPage> =
        httpAPIUtil.callAPIWithErrorHandling<StoryViewersPage, GetStoryViewersResponse>(
            callAPI = {
                httpClient.get(ThreadRoute.StoryViewers(storyId, page, limit).url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun replyToStory(
        storyId: Long,
        replyText: String
    ): Resource<StoryReplyResult> =
        httpAPIUtil.callAPIWithErrorHandling<StoryReplyResult, StoryReplyResponse>(
            callAPI = {
                httpClient.post(ThreadRoute.StoryReply.url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                    setBody(
                        StoryReplyRequest(
                            story_id = storyId,
                            reply_text = replyText
                        )
                    )
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    override suspend fun getStoryMentionCandidates(
        query: String,
        limit: Int
    ): Resource<List<String>> =
        httpAPIUtil.callAPIWithErrorHandling<List<String>, GetStoryMentionCandidatesResponse>(
            callAPI = {
                httpClient.get(ThreadRoute.StoryMentions.url) {
                    contentType(ContentType.Application.Json)
                    appendTokenToHeader(sPrefManager.getToken())
                    parameter("q", query)
                    parameter("limit", limit)
                }
            },
            callRefreshTokenAPI = this::getToken
        )

    private fun resolveVideoDurationSeconds(mediaFile: CreatePostMedia): Double? {
        val durationMillis = mediaMetadataExtractor.extract(
            PlatformMediaReference(mediaFile.file.handle.id)
        ).durationMs ?: return null
        return durationMillis / 1000.0
    }

    private fun createMultipartBoundary(): String {
        val randomToken = CommonUuid.randomUuidString().replace("-", "")
        return "secmsg-$randomToken"
    }

    private suspend fun ensureFreshAccessTokenBeforeUpload() {
        ensureFreshAccessTokenIfNearExpiry(
            sPrefManager = sPrefManager,
            refreshAccessToken = this::getToken,
            logTag = "ThreadApiServiceImpl",
            appClock = appClock,
            appLogger = appLogger,
            base64Codec = base64Codec,
        )
    }
}
