package com.dino.message.corefeature.domain.upload

private val mediaMimeTypesByExtension = mapOf(
    "jpg" to "image/jpeg", "jpeg" to "image/jpeg", "png" to "image/png",
    "gif" to "image/gif", "webp" to "image/webp", "bmp" to "image/bmp",
    "heic" to "image/heic", "heif" to "image/heif", "avif" to "image/avif",
    "svg" to "image/svg+xml", "svgz" to "image/svg+xml",
    "mp4" to "video/mp4", "m4v" to "video/mp4", "mov" to "video/quicktime",
    "webm" to "video/webm", "mkv" to "video/x-matroska", "avi" to "video/x-msvideo",
    "3gp" to "video/3gpp", "3gpp" to "video/3gpp", "ogv" to "video/ogg",
    "mp3" to "audio/mpeg", "m4a" to "audio/mp4", "aac" to "audio/aac",
    "flac" to "audio/flac", "wav" to "audio/wav", "ogg" to "audio/ogg",
    "oga" to "audio/ogg", "opus" to "audio/opus", "aiff" to "audio/aiff",
)

fun resolveMediaFileMimeType(displayName: String, reportedMimeType: String?): String {
    val reported = MediaMimePolicy.normalizeMimeType(reportedMimeType)
    if (reported.isNotEmpty() && reported != "application/octet-stream") return reported
    return mediaMimeTypesByExtension[displayName.substringAfterLast('.', "").lowercase()]
        ?: "application/octet-stream"
}
