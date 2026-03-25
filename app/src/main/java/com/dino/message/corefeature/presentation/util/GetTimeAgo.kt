package com.dino.message.corefeature.presentation.util

import android.annotation.SuppressLint
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs

@SuppressLint("NewApi")
fun getTimeAgo(dateString: String): String {

    val normalized = dateString.trimEnd('Z')

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")

    val localDateTime = LocalDateTime.parse(normalized, formatter)
    val instant = localDateTime.toInstant(ZoneOffset.UTC)

    val now = Instant.now()

    val seconds = Duration.between(instant, now)
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

    if (seconds > 0) return "${seconds}s"

    return "now"
}
