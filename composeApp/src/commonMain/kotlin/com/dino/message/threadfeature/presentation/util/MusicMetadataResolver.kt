package com.dino.message.threadfeature.presentation.util

data class ResolvedMusicMetadata(
    val title: String,
    val artist: String?,
)

fun resolveMusicMetadata(
    displayName: String?,
    title: String?,
    artist: String?,
    titleFallback: String = "Music track",
): ResolvedMusicMetadata {
    val normalizedDisplayName = normalizeMusicText(displayName)
    val normalizedTitle = normalizeMusicText(title)
    val normalizedArtist = normalizeMusicText(artist)
    val (parsedArtist, parsedTitle) = splitMusicMetadataFromDisplayName(normalizedDisplayName)

    val resolvedTitle = normalizedTitle
        ?: parsedTitle
        ?: normalizedDisplayName?.substringBeforeLast('.', missingDelimiterValue = normalizedDisplayName)
        ?: titleFallback

    var resolvedArtist = normalizedArtist ?: parsedArtist
    if (!resolvedArtist.isNullOrBlank() && resolvedArtist.equals(resolvedTitle, ignoreCase = true)) {
        resolvedArtist = null
    }

    return ResolvedMusicMetadata(
        title = resolvedTitle,
        artist = resolvedArtist
    )
}

fun artistTextOrUnknown(artist: String?): String {
    return normalizeMusicText(artist) ?: "Unknown artist"
}

private fun splitMusicMetadataFromDisplayName(displayName: String?): Pair<String?, String?> {
    val baseName = normalizeMusicText(
        displayName?.substringBeforeLast('.', missingDelimiterValue = displayName)
    ) ?: return null to null

    val separators = listOf(" - ", " \u2013 ", " \u2014 ")
    for (separator in separators) {
        if (!baseName.contains(separator)) continue
        val parts = baseName.split(separator, limit = 2)
        if (parts.size < 2) continue
        val candidateArtist = normalizeMusicText(parts[0])
        val candidateTitle = normalizeMusicText(parts[1])
        if (!candidateArtist.isNullOrBlank() && !candidateTitle.isNullOrBlank()) {
            return candidateArtist to candidateTitle
        }
    }

    return null to baseName
}

private fun normalizeMusicText(value: String?): String? {
    val normalized = value?.trim().orEmpty()
    return normalized.ifBlank { null }
}
