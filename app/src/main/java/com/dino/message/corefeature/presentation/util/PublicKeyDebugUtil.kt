package com.dino.message.corefeature.presentation.util

import java.security.MessageDigest

fun formatPublicKeyForLog(rawKey: String?): String {
    if (rawKey == null) return "key=null"

    val trimmed = rawKey.trim()
    val escaped = trimmed
        .replace("\r", "\\r")
        .replace("\n", "\\n")
    val head = escaped.take(220)
    val tail = if (escaped.length > 300) escaped.takeLast(120) else ""
    val sha256 = runCatching {
        MessageDigest.getInstance("SHA-256")
            .digest(trimmed.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(16)
    }.getOrElse { "n/a" }

    return buildString {
        append("len=").append(rawKey.length)
        append(", trimmedLen=").append(trimmed.length)
        append(", sha256=").append(sha256)
        append(", keyHead=").append(head)
        if (tail.isNotEmpty()) {
            append(", keyTail=").append(tail)
        }
    }
}
