package com.dino.message.threadfeature.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class StoryFeed(
    val user_stories: List<StoryUserFeedItem>,
    val generated_at: String? = null
)

@Serializable
data class StoryUserFeedItem(
    val user_id: Long,
    val username: String,
    val name: String? = null,
    val badge: String? = null,
    val avatar: String? = null,
    val profile_image_shape: String? = null,
    val has_unseen_story: Boolean = false,
    val story_count: Int = 0,
    val first_story_timestamp: String? = null,
    val latest_story_timestamp: String? = null,
    val story_ids: List<Long> = emptyList()
)

@Serializable
data class StoryBundle(
    val story: StoryDetail,
    val owner: StoryOwner,
    val user_stories: List<StoryDetail>
)

@Serializable
data class StoryOwner(
    val user_id: Long,
    val username: String,
    val name: String? = null,
    val badge: String? = null
)

@Serializable
data class StoryDetail(
    val story_id: Long,
    val user_id: Long,
    val media_url: String,
    val media_type: String,
    val created_at: String? = null,
    val expires_at: String? = null,
    val mention_user_ids: List<Long> = emptyList(),
    val view_count: Int = 0,
    val like_count: Int = 0,
    val viewer_has_viewed: Boolean = false,
    val viewer_liked: Boolean = false
)

@Serializable
data class StoryUploadResult(
    val story_id: Long,
    val media_url: String,
    val media_type: String,
    val expires_at: String? = null,
    val mention_user_ids: List<Long> = emptyList()
)

@Serializable
data class StoryViewResult(
    val story_id: Long,
    val recorded: Boolean,
    val view_count: Int,
    val created: Boolean,
    val queued: Boolean
)

@Serializable
data class StoryLikeResult(
    val story_id: Long,
    val liked: Boolean,
    val like_count: Int,
    val view_count: Int
)

@Serializable
data class StoryReplyResult(
    val story_id: Long,
    val recipient: String,
    val message: String
)

@Serializable
data class StoryViewersPage(
    val story_id: Long,
    val page: Int,
    val limit: Int,
    val total: Int,
    val viewers: List<StoryViewer>
)

@Serializable
data class StoryViewer(
    val viewer_id: Long,
    val username: String,
    val badge: String? = null,
    val avatar: String? = null,
    val profile_image_shape: String? = null,
    val viewed_at: String? = null,
    val liked: Boolean = false
)
