package com.dino.message.corefeature.data.upload

import com.dino.message.chatfeature.domain.model.MessageAttachment
import org.json.JSONObject

internal fun MessageAttachment.toSocketAttachment() = JSONObject().apply {
    put("type", type)
    put("url", url)
    put("mime_type", mimeType)
    put("file_name", fileName)
    put("size_bytes", sizeBytes)
}
