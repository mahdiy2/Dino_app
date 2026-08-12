package com.dino.message.mainpage.domain.model

data class PaginatedContacts(
    val contacts: List<DetailedContact>,
    val page: Int,
    val limit: Int,
    val total: Int
) {
    val hasMore: Boolean get() = page * limit < total
}

data class DetailedContact(
    val username: String,
    val name: String? = null,
    val badge: String? = null,
    val profileImageUrl: String? = null,
    val profileImageShape: String? = null,
    val profileVideoUrl: String? = null,
    val hasMessage: Boolean = false,
    val online: Boolean = false,
    val lastMessage: ContactLastMessage? = null
)

data class ContactLastMessage(
    val from: String,
    val type: String,
    val timestamp: String
)
