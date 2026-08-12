package com.dino.message.chatfeature.data.remote.dto

import com.dino.message.chatfeature.domain.model.MessageAttachment as DomainMessageAttachment
import com.dino.message.chatfeature.domain.model.SenderSummary as DomainSenderSummary
import com.dino.message.chatfeature.domain.model.StoryReferencePreview as DomainStoryReferencePreview
import com.dino.message.chatfeature.domain.model.UnreadSummary

fun MessageAttachment.toDomain(): DomainMessageAttachment = DomainMessageAttachment(
    type = type,
    url = url,
    mimeType = mimeType,
    fileName = fileName,
    sizeBytes = sizeBytes,
    objectName = objectName,
    storyId = storyId,
    storyLink = storyLink,
    storyMediaType = storyMediaType,
    storyMediaUrl = storyMediaUrl,
    storyReferencePreview = storyReferencePreview?.toDomain(),
)

fun StoryReferencePreview.toDomain(): DomainStoryReferencePreview = DomainStoryReferencePreview(
    storyId = storyId,
    mediaType = mediaType,
    mediaUrl = mediaUrl,
)

fun UnreadResponse.toDomain(): UnreadSummary = UnreadSummary(
    total = total,
    senders = senders.map { it.toDomain() },
)

fun SenderSummary.toDomain(): DomainSenderSummary = DomainSenderSummary(
    sender = sender,
    count = count,
    lastType = lastType,
    lastTimestamp = lastTimestamp,
)
