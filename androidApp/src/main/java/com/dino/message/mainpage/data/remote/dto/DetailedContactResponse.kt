package com.dino.message.mainpage.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DetailedContactResponse(
    val contacts: List<DetailedContact>,
    val page: Int = 1,
    val limit: Int = 20,
    val total: Int = 0
) : ResponseToResultMapper<PaginatedContacts> {
    override fun mapResponseToResult(): PaginatedContacts = PaginatedContacts(
        contacts = contacts,
        page = page,
        limit = limit,
        total = total
    )
}

data class PaginatedContacts(
    val contacts: List<DetailedContact>,
    val page: Int,
    val limit: Int,
    val total: Int
) {
    val hasMore: Boolean get() = page * limit < total
}

@Serializable
data class DetailedContact(
    val username: String,
    val name: String? = null,
    val badge: String? = null,
    @SerialName("profile_image_url") val profileImageUrl: String? = null,
    @SerialName("profile_image_shape") val profileImageShape: String? = null,
    @SerialName("profile_video_url") val profileVideoUrl: String? = null,
    @SerialName("has_message") val hasMessage: Boolean = false,
    val online: Boolean = false,
    @SerialName("last_message") val lastMessage: ContactLastMessage? = null
)

@Serializable
data class ContactLastMessage(
    val from: String,
    val type: String,
    val timestamp: String
)
