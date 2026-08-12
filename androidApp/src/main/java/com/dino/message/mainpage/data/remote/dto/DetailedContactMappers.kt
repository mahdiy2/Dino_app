package com.dino.message.mainpage.data.remote.dto

import com.dino.message.mainpage.domain.model.ContactLastMessage as DomainContactLastMessage
import com.dino.message.mainpage.domain.model.DetailedContact as DomainDetailedContact
import com.dino.message.mainpage.domain.model.PaginatedContacts as DomainPaginatedContacts

fun DetailedContact.toDomain(): DomainDetailedContact = DomainDetailedContact(
    username = username,
    name = name,
    badge = badge,
    profileImageUrl = profileImageUrl,
    profileImageShape = profileImageShape,
    profileVideoUrl = profileVideoUrl,
    hasMessage = hasMessage,
    online = online,
    lastMessage = lastMessage?.toDomain(),
)

fun ContactLastMessage.toDomain(): DomainContactLastMessage = DomainContactLastMessage(
    from = from,
    type = type,
    timestamp = timestamp,
)

fun PaginatedContacts.toDomain(): DomainPaginatedContacts = DomainPaginatedContacts(
    contacts = contacts.map { it.toDomain() },
    page = page,
    limit = limit,
    total = total,
)
