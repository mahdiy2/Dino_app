package com.dino.message.threadfeature.data.remote.dto

import com.dino.message.corefeature.data.remote.ResponseToResultMapper
import kotlinx.serialization.Serializable

@Serializable
data class StoryMentionCandidateDto(
    val username: String,
)

@Serializable
data class GetStoryMentionCandidatesResponse(
    val users: List<StoryMentionCandidateDto> = emptyList(),
) : ResponseToResultMapper<List<String>> {
    override fun mapResponseToResult(): List<String> = users
        .asSequence()
        .map { it.username.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .toList()
}
