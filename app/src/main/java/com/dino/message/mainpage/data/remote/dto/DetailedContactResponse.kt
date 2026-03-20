package com.dino.message.mainpage.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DetailedContactResponse(
    val contacts: List<DetailedContact>
) : ResponseToResultMapper<List<DetailedContact>> {
    override fun mapResponseToResult(): List<DetailedContact> = contacts
}

@Serializable
data class DetailedContact(
    val username: String,
    val name: String? = null,
    @SerialName("profile_image_url") val profileImageUrl: String? = null,
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
