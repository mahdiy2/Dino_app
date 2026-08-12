package com.dino.message.chatfeature.data.local

import com.dino.message.chatfeature.domain.model.MessageEntity

fun MessageEntity.toLocalEntity(): MessageLocalEntity = MessageLocalEntity(
    id = id,
    messageId = messageId,
    chatId = chatId,
    senderOrReceiver = senderOrReceiver,
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

fun MessageLocalEntity.toDomainEntity(): MessageEntity = MessageEntity(
    id = id,
    messageId = messageId,
    chatId = chatId,
    senderOrReceiver = senderOrReceiver,
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
