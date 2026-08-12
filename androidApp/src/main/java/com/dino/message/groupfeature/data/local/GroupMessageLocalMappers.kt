package com.dino.message.groupfeature.data.local

import com.dino.message.groupfeature.domain.model.GroupMessageEntity

fun GroupMessageEntity.toLocalEntity(): GroupMessageLocalEntity = GroupMessageLocalEntity(
    id = id,
    messageId = messageId,
    groupId = groupId,
    sender = sender,
    content = content,
    timestamp = timestamp,
    messageType = messageType,
    attachmentType = attachmentType,
    attachmentUrl = attachmentUrl,
    attachmentMimeType = attachmentMimeType,
    attachmentFileName = attachmentFileName,
    attachmentSizeBytes = attachmentSizeBytes,
    isSeen = isSeen,
    replyToMessageId = replyToMessageId,
    replyToSender = replyToSender,
    replyPreview = replyPreview,
    reactionsJson = reactionsJson,
)

fun GroupMessageLocalEntity.toDomainEntity(): GroupMessageEntity = GroupMessageEntity(
    id = id,
    messageId = messageId,
    groupId = groupId,
    sender = sender,
    content = content,
    timestamp = timestamp,
    messageType = messageType,
    attachmentType = attachmentType,
    attachmentUrl = attachmentUrl,
    attachmentMimeType = attachmentMimeType,
    attachmentFileName = attachmentFileName,
    attachmentSizeBytes = attachmentSizeBytes,
    isSeen = isSeen,
    replyToMessageId = replyToMessageId,
    replyToSender = replyToSender,
    replyPreview = replyPreview,
    reactionsJson = reactionsJson,
)
