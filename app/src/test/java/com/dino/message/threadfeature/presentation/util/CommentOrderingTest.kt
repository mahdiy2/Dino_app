package com.dino.message.threadfeature.presentation.util

import com.dino.message.threadfeature.domain.model.Author
import com.dino.message.threadfeature.domain.model.Comment
import org.junit.Assert.assertEquals
import org.junit.Test

class CommentOrderingTest {

    @Test
    fun ordersByScoreDescendingThenNewerCreatedAt() {
        val older = comment(
            id = 1L,
            score = 2L,
            createdAt = "2026-04-08T10:00:00Z"
        )
        val newerSameScore = comment(
            id = 2L,
            score = 2L,
            createdAt = "2026-04-08T11:00:00Z"
        )
        val highestScore = comment(
            id = 3L,
            score = 5L,
            createdAt = "2026-04-08T09:00:00Z"
        )

        val ordered = orderCommentsByScoreAndRecency(listOf(older, highestScore, newerSameScore))

        assertEquals(listOf(3L, 2L, 1L), ordered.map { it.id })
    }

    @Test
    fun appliesSameOrderingToRepliesRecursively() {
        val parent = comment(
            id = 10L,
            score = 0L,
            createdAt = "2026-04-08T10:00:00Z",
            replies = listOf(
                comment(id = 20L, score = 1L, createdAt = "2026-04-08T10:00:00Z"),
                comment(id = 21L, score = 1L, createdAt = "2026-04-08T11:00:00Z"),
                comment(id = 22L, score = 3L, createdAt = "2026-04-08T09:00:00Z")
            )
        )

        val ordered = orderCommentsByScoreAndRecency(listOf(parent))

        assertEquals(listOf(22L, 21L, 20L), ordered.first().replies.map { it.id })
    }

    private fun comment(
        id: Long,
        score: Long,
        createdAt: String,
        replies: List<Comment> = emptyList()
    ): Comment {
        return Comment(
            id = id,
            author = Author(
                id = id,
                username = "user$id",
                name = "User $id",
                profile_image_url = null
            ),
            text = "comment-$id",
            score = score,
            created_at = createdAt,
            parent_id = null,
            replies = replies
        )
    }
}
