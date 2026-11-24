package com.dino.message.threadfeature.presentation.component

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.dino.message.threadfeature.domain.model.CreatePostMedia

enum class TextAlignOption {
    Left,
    Center,
    Right,
}

data class DrawStroke(
    val points: List<Offset>,
    val color: Color,
    val widthPx: Float,
)

data class TextOverlay(
    val id: String,
    val text: String,
    val offset: Offset,
    val color: Color,
    val backgroundColor: Color,
    val fontSizeSp: Float,
    val isBold: Boolean,
    val isItalic: Boolean,
    val align: TextAlignOption,
    val boxWidthPx: Float = 220f,
)

data class MentionOverlay(
    val id: String,
    val username: String,
    val offset: Offset,
)

data class EditorSnapshot(
    val zoom: Float,
    val panX: Float,
    val panY: Float,
    val drawStrokes: List<DrawStroke>,
    val textOverlays: List<TextOverlay>,
    val mentionOverlays: List<MentionOverlay>,
)

data class StoryUploadIntent(val media: CreatePostMedia, val mentionUsernames: List<String>)

fun storyMentionUsernames(overlays: List<MentionOverlay>): List<String> =
    overlays.map { it.username.trim() }.filter { it.isNotEmpty() }.distinct()

class StoryEditingDraft {
    val drawStrokes = mutableStateListOf<DrawStroke>()
    val textOverlays = mutableStateListOf<TextOverlay>()
    val mentionOverlays = mutableStateListOf<MentionOverlay>()
    val history = StoryEditHistory()

    fun snapshot(zoom: Float, panX: Float, panY: Float) = EditorSnapshot(
        zoom = zoom,
        panX = panX,
        panY = panY,
        drawStrokes = drawStrokes.map { it.copy(points = it.points.toList()) },
        textOverlays = textOverlays.toList(),
        mentionOverlays = mentionOverlays.toList(),
    )

    fun restoreOverlays(snapshot: EditorSnapshot) {
        drawStrokes.clear()
        drawStrokes.addAll(snapshot.drawStrokes)
        textOverlays.clear()
        textOverlays.addAll(snapshot.textOverlays)
        mentionOverlays.clear()
        mentionOverlays.addAll(snapshot.mentionOverlays)
    }
}

class StoryEditHistory {
    private val undo = mutableStateListOf<EditorSnapshot>()
    private val redo = mutableStateListOf<EditorSnapshot>()
    val canUndo: Boolean get() = undo.isNotEmpty()
    val canRedo: Boolean get() = redo.isNotEmpty()

    fun record(snapshot: EditorSnapshot) {
        undo.add(snapshot)
        if (undo.size > 70) undo.removeAt(0)
        redo.clear()
    }

    fun undo(current: EditorSnapshot): EditorSnapshot? {
        val previous = undo.removeLastOrNull() ?: return null
        redo.add(current)
        return previous
    }

    fun redo(current: EditorSnapshot): EditorSnapshot? {
        val next = redo.removeLastOrNull() ?: return null
        undo.add(current)
        return next
    }
}
