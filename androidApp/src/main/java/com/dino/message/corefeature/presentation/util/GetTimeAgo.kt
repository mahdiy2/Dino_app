package com.dino.message.corefeature.presentation.util

import android.annotation.SuppressLint
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.ZonedDateTime

@SuppressLint("NewApi")
fun getTimeAgo(dateString: String): String {
    val instant = parseServerInstant(dateString) ?: return "0s"

    val seconds = Duration.between(instant, Instant.now())
        .seconds
        .coerceAtLeast(0)

    val years = seconds / (60 * 60 * 24 * 365)
    if (years > 0) return "${years}y"

    val months = seconds / (60 * 60 * 24 * 30)
    if (months > 0) return "${months}mo"

    val weeks = seconds / (60 * 60 * 24 * 7)
    if (weeks > 0) return "${weeks}w"

    val days = seconds / (60 * 60 * 24)
    if (days > 0) return "${days}d"

    val hours = seconds / 3600
    if (hours > 0) return "${hours}h"

    val minutes = seconds / 60
    if (minutes > 0) return "${minutes}m"

    return if (seconds > 0) "${seconds}s" else "0s"
}

@SuppressLint("NewApi")
fun getRelativeTimeLabel(dateString: String?): String {
    val relative = dateString?.let(::getTimeAgo) ?: return "just now"
    return if (relative == "0s") "just now" else "$relative ago"
}

@SuppressLint("NewApi")
fun parseServerInstant(dateString: String?): Instant? {
    val raw = dateString?.trim().orEmpty()
    if (raw.isEmpty()) return null

    try {
        return Instant.parse(raw)
    } catch (_: DateTimeParseException) {
    }

    try {
        return OffsetDateTime.parse(raw, DateTimeFormatter.ISO_DATE_TIME).toInstant()
    } catch (_: DateTimeParseException) {
    }

    try {
        return ZonedDateTime.parse(raw, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
    } catch (_: DateTimeParseException) {
    }

    val formatters = listOf(
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    )

    for (formatter in formatters) {
        try {
            return LocalDateTime.parse(raw.trimEnd('Z'), formatter).toInstant(ZoneOffset.UTC)
        } catch (_: DateTimeParseException) {
        }
    }

    return null
}
