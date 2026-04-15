package com.dino.message.corefeature.presentation.util

private const val SHARE_PREFIX = "[POST_SHARE]"
private const val SHARE_SEPARATOR = "|"

data class PostSharePayload(
    val postId: Long,
    val authorUsername: String,
    val previewText: String,
)

fun encodePostShareMessage(
    postId: Long,
    authorUsername: String,
    postText: String,
): String {
    val normalizedPreview = postText
        .replace('\n', ' ')
        .replace(SHARE_SEPARATOR, " ")
        .trim()
        .take(240)

    return listOf(
        SHARE_PREFIX,
        postId.toString(),
        authorUsername.trim(),
        normalizedPreview,
    ).joinToString(separator = SHARE_SEPARATOR)
}

fun decodePostShareMessage(raw: String): PostSharePayload? {
    if (!raw.startsWith("$SHARE_PREFIX$SHARE_SEPARATOR")) return null

    val parts = raw.split(SHARE_SEPARATOR, limit = 4)
    if (parts.size < 4) return null

    val postId = parts[1].toLongOrNull() ?: return null
    val authorUsername = parts[2].trim()
    if (authorUsername.isBlank()) return null

    return PostSharePayload(
        postId = postId,
        authorUsername = authorUsername,
        previewText = parts[3].trim(),
    )
}
