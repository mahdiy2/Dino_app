package com.dino.message.threadfeature.presentation.util

import com.dino.message.corefeature.presentation.util.parseServerInstant
import com.dino.message.threadfeature.domain.model.Comment

internal fun orderCommentsByScoreAndRecency(comments: List<Comment>): List<Comment> {
    return comments
        .map { comment ->
            comment.copy(replies = orderCommentsByScoreAndRecency(comment.replies))
        }
        .sortedWith(
            compareByDescending<Comment> { it.score }
                .thenByDescending { parseServerInstant(it.created_at)?.toEpochMilli() ?: Long.MIN_VALUE }
        )
}
