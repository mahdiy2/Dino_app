package com.dino.message.spashscreen.domain.model

data class VersionCheckDecision(
    val action: String,
    val isBlocking: Boolean,
    val title: String,
    val message: String,
    val downloadUrl: String,
    val normalizedVersion: String,
)
