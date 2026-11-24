package com.dino.message.threadfeature.presentation.component

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StoryEditingDraftTest {
    @Test
    fun historyRestoresMentionsAndCrop() {
        val draft = StoryEditingDraft()
        draft.history.record(draft.snapshot(1f, 0f, 0f))
        draft.mentionOverlays.add(MentionOverlay("a", " alice ", Offset.Zero))
        draft.mentionOverlays.add(MentionOverlay("b", "alice", Offset.Zero))
        assertEquals(listOf("alice"), storyMentionUsernames(draft.mentionOverlays))

        val previous = assertNotNull(draft.history.undo(draft.snapshot(2f, 1f, 0f)))
        draft.restoreOverlays(previous)
        assertTrue(draft.mentionOverlays.isEmpty())
        val redo = assertNotNull(draft.history.redo(draft.snapshot(1f, 0f, 0f)))
        draft.restoreOverlays(redo)
        assertEquals(2f, redo.zoom)
        assertEquals(listOf("alice"), storyMentionUsernames(draft.mentionOverlays))
    }
}
