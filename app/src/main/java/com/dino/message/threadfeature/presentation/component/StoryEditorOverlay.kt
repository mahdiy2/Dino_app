package com.dino.message.threadfeature.presentation.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.util.TypedValue
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.FormatColorFill
import androidx.compose.material.icons.rounded.FormatColorText
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Redo
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.Crop
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Effects
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.dino.message.R
import com.dino.message.threadfeature.domain.model.CreatePostMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val TARGET_ASPECT = 9f / 16f
private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 5f
private const val DEFAULT_EXPORT_WIDTH = 1080
private const val DEFAULT_EXPORT_HEIGHT = 1920
private const val MAX_TEXT_LENGTH = 200

private enum class ColorTarget {
    Draw,
    Text,
    TextBackground,
}

private enum class EditorTool {
    Crop,
    Draw,
}

private enum class TextAlignOption {
    Left,
    Center,
    Right,
}

private data class DrawStroke(
    val points: List<Offset>,
    val color: Color,
    val widthPx: Float,
)

private data class TextOverlay(
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

private data class MentionOverlay(
    val id: String,
    val username: String,
    val offset: Offset,
)

private data class EditorSnapshot(
    val zoom: Float,
    val panX: Float,
    val panY: Float,
    val drawStrokes: List<DrawStroke>,
    val textOverlays: List<TextOverlay>,
    val mentionOverlays: List<MentionOverlay>,
)

@Composable
fun StoryEditorOverlay(
    media: CreatePostMedia,
    mentionSuggestions: List<String>,
    onMentionQueryChanged: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onPost: (CreatePostMedia, List<String>) -> Unit,
    onEditorNotice: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    val context = LocalContext.current
    val isImage = media.mimeType.lowercase(Locale.US).startsWith("image/")

    var isPreviewMode by rememberSaveable(media.uri.toString()) { mutableStateOf(false) }
    var isExporting by rememberSaveable(media.uri.toString()) { mutableStateOf(false) }
    var exportProgressPercent by rememberSaveable(media.uri.toString()) { mutableStateOf<Int?>(null) }
    var zoom by rememberSaveable(media.uri.toString()) { mutableFloatStateOf(1f) }
    var panX by rememberSaveable(media.uri.toString()) { mutableFloatStateOf(0f) }
    var panY by rememberSaveable(media.uri.toString()) { mutableFloatStateOf(0f) }
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    var drawingColor by remember { mutableStateOf(Color.White) }
    var textColor by remember { mutableStateOf(Color.White) }
    var textBackgroundColor by remember { mutableStateOf(Color.Black) }
    var colorTarget by remember { mutableStateOf(ColorTarget.Draw) }
    var showColorDialog by rememberSaveable { mutableStateOf(false) }

    var selectedTool by rememberSaveable { mutableStateOf(EditorTool.Crop) }
    var showMentionDialog by rememberSaveable { mutableStateOf(false) }
    var textBold by rememberSaveable { mutableStateOf(false) }
    var textItalic by rememberSaveable { mutableStateOf(false) }
    var textAlign by remember { mutableStateOf(TextAlignOption.Left) }
    var activeTextInputId by rememberSaveable { mutableStateOf<String?>(null) }

    var activeOverlayId by rememberSaveable { mutableStateOf<String?>(null) }

    val drawHistory = remember { mutableStateListOf<DrawStroke>() }
    val textOverlays = remember { mutableStateListOf<TextOverlay>() }
    val mentionOverlays = remember { mutableStateListOf<MentionOverlay>() }
    val undoHistory = remember { mutableStateListOf<EditorSnapshot>() }
    val redoHistory = remember { mutableStateListOf<EditorSnapshot>() }

    var activeStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    fun captureSnapshot(): EditorSnapshot {
        return EditorSnapshot(
            zoom = zoom,
            panX = panX,
            panY = panY,
            drawStrokes = drawHistory.map { it.copy(points = it.points.toList()) },
            textOverlays = textOverlays.toList(),
            mentionOverlays = mentionOverlays.toList()
        )
    }

    fun restoreSnapshot(snapshot: EditorSnapshot) {
        zoom = snapshot.zoom
        panX = snapshot.panX
        panY = snapshot.panY
        drawHistory.clear()
        drawHistory.addAll(snapshot.drawStrokes)
        textOverlays.clear()
        textOverlays.addAll(snapshot.textOverlays)
        mentionOverlays.clear()
        mentionOverlays.addAll(snapshot.mentionOverlays)
    }

    fun pushUndoSnapshot() {
        undoHistory.add(captureSnapshot())
        if (undoHistory.size > 70) {
            undoHistory.removeAt(0)
        }
        redoHistory.clear()
    }

    Surface(
        modifier = modifier
            .fillMaxSize(),
        color = Color.Black.copy(alpha = 0.95f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = statusBarPadding)
            ) {
                StoryEditorTopBar(
                    isExporting = isExporting,
                    canUndo = undoHistory.isNotEmpty(),
                    canRedo = redoHistory.isNotEmpty(),
                    showColorPicker = selectedTool == EditorTool.Draw,
                    showCropHint = selectedTool == EditorTool.Crop,
                    onClose = onDismiss,
                    onUndo = {
                        val snapshot = undoHistory.removeLastOrNull() ?: return@StoryEditorTopBar
                        redoHistory.add(captureSnapshot())
                        restoreSnapshot(snapshot)
                        activeTextInputId = null
                    },
                    onRedo = {
                        val snapshot = redoHistory.removeLastOrNull() ?: return@StoryEditorTopBar
                        undoHistory.add(captureSnapshot())
                        restoreSnapshot(snapshot)
                        activeTextInputId = null
                    },
                    onColorPickerClick = {
                        if (selectedTool == EditorTool.Draw) {
                            colorTarget = ColorTarget.Draw
                            showColorDialog = true
                        }
                    },
                    onPost = {
                        if (isExporting) return@StoryEditorTopBar
                        coroutineScope.launch {
                            isExporting = true
                            exportProgressPercent = if (isImage) null else 0
                            try {
                                val mentionUsernames = mentionOverlays.map { it.username }.distinct()
                                if (isImage) {
                                    val exported = exportEditedStoryImage(
                                        context = context,
                                        source = media,
                                        viewportSize = viewportSize,
                                        zoom = zoom,
                                        pan = Offset(panX, panY),
                                        drawStrokes = drawHistory,
                                        textOverlays = textOverlays,
                                        mentionOverlays = mentionOverlays,
                                    )
                                    if (exported != null) {
                                        onPost(exported, mentionUsernames)
                                    }
                                } else {
                                    val exported = exportEditedStoryVideo(
                                        context = context,
                                        source = media,
                                        viewportSize = viewportSize,
                                        zoom = zoom,
                                        pan = Offset(panX, panY),
                                        drawStrokes = drawHistory,
                                        textOverlays = textOverlays,
                                        mentionOverlays = mentionOverlays,
                                        onProgress = { progress -> exportProgressPercent = progress },
                                    )
                                    if (exported == null) {
                                        onEditorNotice(
                                            "Couldn't render edited video. Uploaded original video instead."
                                        )
                                    }
                                    onPost(exported ?: media, mentionUsernames)
                                }
                            } finally {
                                isExporting = false
                                exportProgressPercent = null
                            }
                        }
                    }
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    StoryCanvas(
                        media = media,
                        isImage = isImage,
                        zoom = zoom,
                        pan = Offset(panX, panY),
                        previewMode = isPreviewMode,
                        drawStrokes = drawHistory,
                        activeStroke = activeStroke,
                        textOverlays = textOverlays,
                        mentionOverlays = mentionOverlays,
                        activeOverlayId = activeOverlayId,
                        activeTextInputId = activeTextInputId,
                        onActiveOverlayChanged = { activeOverlayId = it },
                        onTextOverlayClick = { id ->
                            activeOverlayId = id
                            activeTextInputId = id
                        },
                        onTextChanged = { id, text ->
                            pushUndoSnapshot()
                            val index = textOverlays.indexOfFirst { it.id == id }
                            if (index >= 0) {
                                textOverlays[index] = textOverlays[index].copy(text = text.take(MAX_TEXT_LENGTH))
                            }
                        },
                        onTextLayoutWidthChanged = { id, widthPx ->
                            val index = textOverlays.indexOfFirst { it.id == id }
                            if (index >= 0) {
                                val maxWidth = (viewportSize.width * 0.86f).coerceAtLeast(120f)
                                val normalized = widthPx.coerceIn(20f, maxWidth)
                                if (kotlin.math.abs(textOverlays[index].boxWidthPx - normalized) > 1f) {
                                    textOverlays[index] = textOverlays[index].copy(boxWidthPx = normalized)
                                }
                            }
                        },
                        onTransform = { deltaPan, zoomFactor ->
                            val nextZoom = (zoom * zoomFactor).coerceIn(MIN_ZOOM, MAX_ZOOM)
                            val maxPanX = ((nextZoom - 1f) * viewportSize.width / 2f).coerceAtLeast(0f)
                            val maxPanY = ((nextZoom - 1f) * viewportSize.height / 2f).coerceAtLeast(0f)
                            zoom = nextZoom
                            panX = (panX + deltaPan.x).coerceIn(-maxPanX, maxPanX)
                            panY = (panY + deltaPan.y).coerceIn(-maxPanY, maxPanY)
                        },
                        onDrawStrokeProgress = { points ->
                            activeStroke = points
                        },
                        onDrawStrokeCommitted = { points ->
                            if (points.size > 1) {
                                pushUndoSnapshot()
                                drawHistory.add(
                                    DrawStroke(
                                        points = points,
                                        color = drawingColor,
                                        widthPx = 6f,
                                    )
                                )
                            }
                            activeStroke = emptyList()
                        },
                        drawingEnabled = !isPreviewMode && selectedTool == EditorTool.Draw,
                        transformEnabled = !isPreviewMode && selectedTool == EditorTool.Crop,
                        onCanvasSizeChanged = { nextViewportSize ->
                            val previousViewportSize = viewportSize
                            if (nextViewportSize.width > 0 && nextViewportSize.height > 0) {
                                if (
                                    previousViewportSize.width > 0 &&
                                    previousViewportSize.height > 0 &&
                                    previousViewportSize != nextViewportSize
                                ) {
                                    val scaleX = nextViewportSize.width.toFloat() / previousViewportSize.width.toFloat()
                                    val scaleY = nextViewportSize.height.toFloat() / previousViewportSize.height.toFloat()

                                    for (index in textOverlays.indices) {
                                        val current = textOverlays[index]
                                        textOverlays[index] = current.copy(
                                            offset = Offset(
                                                x = current.offset.x * scaleX,
                                                y = current.offset.y * scaleY
                                            ),
                                            boxWidthPx = (current.boxWidthPx * scaleX).coerceAtLeast(120f)
                                        )
                                    }

                                    for (index in mentionOverlays.indices) {
                                        val current = mentionOverlays[index]
                                        mentionOverlays[index] = current.copy(
                                            offset = Offset(
                                                x = current.offset.x * scaleX,
                                                y = current.offset.y * scaleY
                                            )
                                        )
                                    }

                                    for (index in drawHistory.indices) {
                                        val stroke = drawHistory[index]
                                        drawHistory[index] = stroke.copy(
                                            points = stroke.points.map { point ->
                                                Offset(
                                                    x = point.x * scaleX,
                                                    y = point.y * scaleY
                                                )
                                            }
                                        )
                                    }

                                    if (activeStroke.isNotEmpty()) {
                                        activeStroke = activeStroke.map { point ->
                                            Offset(
                                                x = point.x * scaleX,
                                                y = point.y * scaleY
                                            )
                                        }
                                    }

                                    panX *= scaleX
                                    panY *= scaleY
                                }
                                viewportSize = nextViewportSize
                            }
                        },
                        onTextMoveStart = { pushUndoSnapshot() },
                        onTextOverlayTransformed = { id, delta, zoomFactor ->
                            val index = textOverlays.indexOfFirst { it.id == id }
                            if (index >= 0) {
                                val current = textOverlays[index]
                                val maxOverlayWidth = (viewportSize.width * 0.95f).coerceAtLeast(180f)
                                val nextFontSize = (current.fontSizeSp * zoomFactor).coerceIn(16f, 120f)
                                val nextBoxWidth = (current.boxWidthPx * zoomFactor).coerceIn(120f, maxOverlayWidth)
                                textOverlays[index] = current.copy(
                                    offset = current.offset + delta,
                                    fontSizeSp = nextFontSize,
                                    boxWidthPx = nextBoxWidth
                                )
                            }
                        },
                        onMentionMoveStart = { pushUndoSnapshot() },
                        onMentionOverlayMoved = { id, delta ->
                            val index = mentionOverlays.indexOfFirst { it.id == id }
                            if (index >= 0) {
                                val current = mentionOverlays[index]
                                mentionOverlays[index] = current.copy(offset = current.offset + delta)
                            }
                        }
                    )

                    if (!isImage) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 64.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Text(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                text = "Video export includes your crop and overlays.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (!isPreviewMode) {
                    Box(
                        modifier = Modifier
                            .imePadding()
                            .padding(bottom = navBarPadding)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            shape = MaterialTheme.shapes.extraLarge
                        ) {
                            AnimatedContent(
                                targetState = activeTextInputId != null,
                                transitionSpec = {
                                    (
                                        fadeIn(animationSpec = tween(durationMillis = 220)) +
                                            slideInVertically(animationSpec = tween(220)) { it / 3 }
                                        ).togetherWith(
                                        fadeOut(animationSpec = tween(durationMillis = 180)) +
                                            slideOutVertically(animationSpec = tween(180)) { -it / 3 }
                                    )
                                },
                                label = "story-editor-toolbar-swap"
                            ) { isTextEditing ->
                                if (isTextEditing) {
                                    InlineTextToolbar(
                                        textBold = textBold,
                                        textItalic = textItalic,
                                        textBackgroundColor = textBackgroundColor,
                                        onBoldToggle = {
                                            pushUndoSnapshot()
                                            textBold = !textBold
                                            val index = textOverlays.indexOfFirst { it.id == activeTextInputId }
                                            if (index >= 0) {
                                                textOverlays[index] = textOverlays[index].copy(isBold = textBold)
                                            }
                                        },
                                        onItalicToggle = {
                                            pushUndoSnapshot()
                                            textItalic = !textItalic
                                            val index = textOverlays.indexOfFirst { it.id == activeTextInputId }
                                            if (index >= 0) {
                                                textOverlays[index] = textOverlays[index].copy(isItalic = textItalic)
                                            }
                                        },
                                        onColorClick = {
                                            colorTarget = ColorTarget.Text
                                            showColorDialog = true
                                        },
                                        onBackgroundClick = {
                                            colorTarget = ColorTarget.TextBackground
                                            showColorDialog = true
                                        },
                                        onDone = {
                                            val id = activeTextInputId
                                            activeTextInputId = null
                                            if (id != null) {
                                                val index = textOverlays.indexOfFirst { it.id == id }
                                                if (index >= 0 && textOverlays[index].text.isBlank()) {
                                                    pushUndoSnapshot()
                                                    textOverlays.removeAt(index)
                                                }
                                            }
                                        },
                                    )
                                } else {
                                    StoryEditorToolbar(
                                        selectedTool = selectedTool,
                                        onCropClick = {
                                            selectedTool = EditorTool.Crop
                                            activeTextInputId = null
                                        },
                                        onDrawClick = {
                                            selectedTool = EditorTool.Draw
                                            colorTarget = ColorTarget.Draw
                                            showColorDialog = false
                                        },
                                        onTextClick = {
                                            selectedTool = EditorTool.Crop
                                            pushUndoSnapshot()
                                            val id = UUID.randomUUID().toString()
                                            val created = TextOverlay(
                                                id = id,
                                                text = "",
                                                offset = Offset(
                                                    x = (viewportSize.width / 2f).coerceAtLeast(140f),
                                                    y = (viewportSize.height / 2f).coerceAtLeast(220f),
                                                ),
                                                color = textColor,
                                                backgroundColor = textBackgroundColor,
                                                fontSizeSp = 34f,
                                                isBold = textBold,
                                                isItalic = textItalic,
                                                align = TextAlignOption.Left,
                                                boxWidthPx = (viewportSize.width * 0.68f).coerceIn(220f, 760f),
                                            )
                                            textOverlays.add(created)
                                            activeOverlayId = id
                                            activeTextInputId = id
                                        },
                                        onMentionClick = { showMentionDialog = true },
                                    )
                                }
                            }
                        }
                    }
                }

            if (isExporting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(color = Color.White)
                        Text(
                            text = exportProgressPercent
                                ?.let { "Rendering video... $it%" }
                                ?: "Preparing story...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
    }

    if (showColorDialog) {
        UnifiedColorPickerDialog(
            initialColor = when (colorTarget) {
                ColorTarget.Draw -> drawingColor
                ColorTarget.Text -> textColor
                ColorTarget.TextBackground -> textBackgroundColor
            },
            onDismiss = { showColorDialog = false },
            onColorSelected = { color ->
                pushUndoSnapshot()
                when (colorTarget) {
                    ColorTarget.Draw -> drawingColor = color
                    ColorTarget.Text -> {
                        textColor = color
                        val index = textOverlays.indexOfFirst { it.id == activeTextInputId }
                        if (index >= 0) {
                            textOverlays[index] = textOverlays[index].copy(color = color)
                        }
                    }
                    ColorTarget.TextBackground -> {
                        textBackgroundColor = color
                        val index = textOverlays.indexOfFirst { it.id == activeTextInputId }
                        if (index >= 0) {
                            textOverlays[index] = textOverlays[index].copy(backgroundColor = color)
                        }
                    }
                }
                showColorDialog = false
            }
        )
    }

    if (showMentionDialog) {
        MentionPickerDialog(
            suggestions = mentionSuggestions,
            onQueryChange = onMentionQueryChanged,
            onDismiss = { showMentionDialog = false },
            onAddMention = { username ->
                if (username.isBlank()) return@MentionPickerDialog
                pushUndoSnapshot()
                mentionOverlays.add(
                    MentionOverlay(
                        id = UUID.randomUUID().toString(),
                        username = username,
                        offset = Offset(
                            x = (viewportSize.width / 2f).coerceAtLeast(100f),
                            y = (viewportSize.height / 2f).coerceAtLeast(200f),
                        )
                    )
                )
                showMentionDialog = false
            }
        )
    }
    }
}

@Composable
private fun StoryEditorTopBar(
    isExporting: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    showColorPicker: Boolean,
    showCropHint: Boolean,
    onClose: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onColorPickerClick: () -> Unit,
    onPost: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ExpressiveIconButton(onClick = onClose, contentDescription = "Close") {
                Icon(imageVector = Icons.Rounded.Close, contentDescription = null, tint = Color.White)
            }
            if (showColorPicker) {
                ExpressiveIconButton(onClick = onColorPickerClick, contentDescription = "Draw color") {
                    Icon(
                        imageVector = Icons.Rounded.FormatColorFill,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            }
            if (showCropHint) {
                Text(
                    text = "Pinch to crop",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            ExpressiveIconButton(onClick = onUndo, enabled = canUndo, contentDescription = "Undo") {
                Icon(
                    imageVector = Icons.Rounded.Undo,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.alpha(if (canUndo) 1f else 0.4f)
                )
            }
            ExpressiveIconButton(onClick = onRedo, enabled = canRedo, contentDescription = "Redo") {
                Icon(
                    imageVector = Icons.Rounded.Redo,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.alpha(if (canRedo) 1f else 0.4f)
                )
            }
            Button(onClick = onPost, enabled = !isExporting) {
                Text(text = "Post")
                Spacer(modifier = Modifier.width(6.dp))
                Icon(imageVector = Icons.Rounded.Send, contentDescription = null)
            }
        }
    }
}

@Composable
private fun StoryCanvas(
    media: CreatePostMedia,
    isImage: Boolean,
    zoom: Float,
    pan: Offset,
    previewMode: Boolean,
    drawStrokes: List<DrawStroke>,
    activeStroke: List<Offset>,
    textOverlays: List<TextOverlay>,
    mentionOverlays: List<MentionOverlay>,
    activeOverlayId: String?,
    activeTextInputId: String?,
    onActiveOverlayChanged: (String?) -> Unit,
    onTextOverlayClick: (String) -> Unit,
    onTextChanged: (String, String) -> Unit,
    onTextLayoutWidthChanged: (String, Float) -> Unit,
    onTransform: (pan: Offset, zoomFactor: Float) -> Unit,
    onDrawStrokeProgress: (List<Offset>) -> Unit,
    onDrawStrokeCommitted: (List<Offset>) -> Unit,
    drawingEnabled: Boolean,
    transformEnabled: Boolean,
    onCanvasSizeChanged: (IntSize) -> Unit,
    onTextMoveStart: () -> Unit,
    onTextOverlayTransformed: (String, Offset, Float) -> Unit,
    onMentionMoveStart: () -> Unit,
    onMentionOverlayMoved: (String, Offset) -> Unit,
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    var isVideoPlaying by remember(media.uri) { mutableStateOf(true) }
    var videoPlaybackError by remember(media.uri) { mutableStateOf<String?>(null) }
    val composedStrokes = remember(drawStrokes, activeStroke) {
        if (activeStroke.isEmpty()) drawStrokes else {
            val result = drawStrokes.toMutableList()
            result.add(DrawStroke(activeStroke, Color.White, 6f))
            result
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val maxWidthByHeight = maxHeight * TARGET_ASPECT
        val canvasWidth = if (maxWidth <= maxWidthByHeight) maxWidth else maxWidthByHeight
        val canvasHeight = canvasWidth / TARGET_ASPECT

        Box(
            modifier = Modifier
                .width(canvasWidth)
                .height(canvasHeight)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0A0A0A))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                .onSizeChanged(onCanvasSizeChanged)
                .pointerInput(previewMode, transformEnabled) {
                    if (!previewMode && transformEnabled) {
                        detectTransformGestures { _, panChange, zoomChange, _ ->
                            onTransform(panChange, zoomChange)
                        }
                    }
                }
        ) {
        if (isImage) {
            AsyncImage(
                model = media.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = 1.08f,
                        scaleY = 1.08f
                    )
                    .blur(30.dp)
                    .alpha(0.9f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f))
            )
            AsyncImage(
                model = media.uri,
                contentDescription = "Story media",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = zoom,
                        scaleY = zoom,
                        translationX = pan.x,
                        translationY = pan.y
                    )
            )
        } else {
            StoryEditorVideoPlayer(
                mediaUri = media.uri,
                isPlaying = isVideoPlaying,
                onPlayingChanged = { isVideoPlaying = it },
                onPlaybackError = { videoPlaybackError = it },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = zoom,
                        scaleY = zoom,
                        translationX = pan.x,
                        translationY = pan.y
                    )
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f)
            ) {
                IconButton(
                    onClick = {
                        val nextPlaying = !isVideoPlaying
                        if (nextPlaying) {
                            videoPlaybackError = null
                        }
                        isVideoPlaying = nextPlaying
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (isVideoPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (isVideoPlaying) "Pause preview" else "Play preview",
                        tint = Color.White
                    )
                }
            }

            if (!videoPlaybackError.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp, start = 12.dp, end = 12.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f)
                ) {
                    Text(
                        text = videoPlaybackError.orEmpty(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        if (isImage) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(drawingEnabled, previewMode) {
                        if (!previewMode && drawingEnabled) {
                            val points = mutableListOf<Offset>()
                            detectDragGestures(
                                onDragStart = { offset ->
                                    points.clear()
                                    points.add(offset)
                                    onDrawStrokeProgress(points.toList())
                                },
                                onDrag = { change, _ ->
                                    points.add(change.position)
                                    onDrawStrokeProgress(points.toList())
                                },
                                onDragEnd = {
                                    onDrawStrokeCommitted(points.toList())
                                },
                                onDragCancel = {
                                    onDrawStrokeCommitted(points.toList())
                                }
                            )
                        }
                    }
            ) {
                composedStrokes.forEach { stroke ->
                    if (stroke.points.size < 2) return@forEach
                    val path = Path().apply {
                        moveTo(stroke.points.first().x, stroke.points.first().y)
                        for (i in 1 until stroke.points.size) {
                            val prev = stroke.points[i - 1]
                            val current = stroke.points[i]
                            val midX = (prev.x + current.x) / 2f
                            val midY = (prev.y + current.y) / 2f
                            quadraticBezierTo(prev.x, prev.y, midX, midY)
                        }
                    }
                    drawPath(
                        path = path,
                        color = stroke.color,
                        style = Stroke(
                            width = stroke.widthPx,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = PathEffect.cornerPathEffect(8f)
                        )
                    )
                }
            }
        }

        textOverlays.forEach { overlay ->
            val isActive = overlay.id == activeOverlayId
            val horizontalPaddingPx = with(density) { 8.dp.toPx() }
            val verticalPaddingPx = with(density) { 4.dp.toPx() }
            val cornerRadiusPx = with(density) { 10.dp.toPx() }
            val textStyle = TextStyle(
                color = overlay.color,
                fontSize = overlay.fontSizeSp.sp,
                fontWeight = if (overlay.isBold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (overlay.isItalic) FontStyle.Italic else FontStyle.Normal,
                textAlign = TextAlign.Start,
                textDirection = TextDirection.Content
            )
            val previewTextPaint = remember(
                overlay.color,
                overlay.fontSizeSp,
                overlay.isBold,
                overlay.isItalic,
                context.resources.configuration
            ) {
                Paint().apply {
                    isAntiAlias = true
                    color = overlay.color.toArgb()
                    textSize = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_SP,
                        overlay.fontSizeSp,
                        context.resources.displayMetrics
                    )
                    typeface = resolveStoryTextTypeface(
                        context = context,
                        isBold = overlay.isBold,
                        isItalic = overlay.isItalic
                    )
                }
            }
            val previewLayoutMetrics = remember(
                overlay.text,
                overlay.boxWidthPx,
                overlay.fontSizeSp,
                overlay.isBold,
                overlay.isItalic,
                context.resources.configuration
            ) {
                buildWrappedTextLayoutMetrics(
                    text = overlay.text,
                    textPaint = previewTextPaint,
                    boxWidthPx = overlay.boxWidthPx,
                    maxLines = 4
                )
            }
            val baseModifier = Modifier
                .align(Alignment.TopStart)
                .offsetFromCenter(overlay.offset)
                .clip(RoundedCornerShape(10.dp))
                .border(
                    width = if (isActive) 1.dp else 0.dp,
                    color = if (isActive) Color.White else Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
                )
                .pointerInput(overlay.id, previewMode) {
                    if (!previewMode) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            onActiveOverlayChanged(overlay.id)
                            var hasTransformed = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val pressed = event.changes.filter { it.pressed }
                                if (pressed.isEmpty()) break

                                val panDelta = if (pressed.size > 1) {
                                    event.calculatePan()
                                } else {
                                    pressed.first().positionChange()
                                }
                                val zoomFactor = if (pressed.size > 1) {
                                    event.calculateZoom()
                                } else {
                                    1f
                                }

                                val hasPan = panDelta != Offset.Zero
                                val hasZoom = kotlin.math.abs(zoomFactor - 1f) > 0.001f
                                if (hasPan || hasZoom) {
                                    if (!hasTransformed) {
                                        onTextMoveStart()
                                        hasTransformed = true
                                    }
                                    onTextOverlayTransformed(overlay.id, panDelta, zoomFactor)
                                }

                                event.changes.forEach { change ->
                                    if (change.positionChanged()) {
                                        change.consume()
                                    }
                                }
                            }
                            onActiveOverlayChanged(null)
                        }
                    }
                }
                .clickable(enabled = !previewMode) { onTextOverlayClick(overlay.id) }

            if (activeTextInputId == overlay.id && !previewMode) {
                val editorContainerWidthPx = overlay.boxWidthPx + (horizontalPaddingPx * 2f)
                BasicTextField(
                    value = overlay.text,
                    onValueChange = { onTextChanged(overlay.id, it) },
                    modifier = baseModifier
                        .width(with(density) { editorContainerWidthPx.toDp() })
                        .background(
                            if (overlay.backgroundColor.alpha > 0f) overlay.backgroundColor else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    textStyle = textStyle,
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(overlay.color),
                    maxLines = 4,
                    onTextLayout = { layoutResult ->
                        val nextWidth = layoutResult.size.width.toFloat()
                        onTextLayoutWidthChanged(overlay.id, nextWidth)
                    }
                )
            } else {
                val previewWidthPx = previewLayoutMetrics.contentWidth + (horizontalPaddingPx * 2f)
                val previewHeightPx = previewLayoutMetrics.layout.height.toFloat() + (verticalPaddingPx * 2f)
                Canvas(
                    modifier = baseModifier
                        .width(with(density) { previewWidthPx.toDp() })
                        .height(with(density) { previewHeightPx.toDp() })
                ) {
                    drawIntoCanvas { canvas ->
                        drawWrappedTextOverlayToCanvas(
                            canvas = canvas.nativeCanvas,
                            anchorX = 0f,
                            anchorY = 0f,
                            text = overlay.text,
                            textPaint = previewTextPaint,
                            backgroundColor = overlay.backgroundColor,
                            backgroundPaint = Paint().apply {
                                isAntiAlias = true
                                style = Paint.Style.FILL
                            },
                            boxWidthPx = overlay.boxWidthPx,
                            horizontalPaddingPx = horizontalPaddingPx,
                            verticalPaddingPx = verticalPaddingPx,
                            cornerRadiusPx = cornerRadiusPx,
                            maxLines = 4
                        )
                    }
                }
            }
        }

        mentionOverlays.forEach { mention ->
            val isActive = mention.id == activeOverlayId
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offsetFromCenter(mention.offset)
                    .pointerInput(mention.id, previewMode) {
                        if (!previewMode) {
                            detectDragGestures(
                                onDragStart = {
                                    onMentionMoveStart()
                                    onActiveOverlayChanged(mention.id)
                                },
                                onDrag = { _, dragAmount ->
                                    onMentionOverlayMoved(mention.id, dragAmount)
                                },
                                onDragEnd = { onActiveOverlayChanged(null) }
                            )
                        }
                    }
                    .clickable(enabled = !previewMode) { onActiveOverlayChanged(mention.id) },
                shape = RoundedCornerShape(14.dp),
                color = Color.Black.copy(alpha = 0.55f),
                border = if (isActive) {
                    androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                } else {
                    null
                }
            ) {
                Text(
                    text = "@${mention.username}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun StoryEditorVideoPlayer(
    mediaUri: Uri,
    isPlaying: Boolean,
    onPlayingChanged: (Boolean) -> Unit,
    onPlaybackError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember(mediaUri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(mediaUri))
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = true
            prepare()
        }
    }

    LaunchedEffect(isPlaying, exoPlayer) {
        if (isPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                onPlayingChanged(false)
                onPlaybackError(
                    error.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "Video preview failed. You can still post this story."
                )
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                player = exoPlayer
            }
        },
        update = { playerView ->
            playerView.player = exoPlayer
            playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StoryEditorToolbar(
    selectedTool: EditorTool,
    onCropClick: () -> Unit,
    onDrawClick: () -> Unit,
    onTextClick: () -> Unit,
    onMentionClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarIconButton(
            icon = Icons.Rounded.Crop,
            label = "Crop",
            selected = selectedTool == EditorTool.Crop,
            onClick = onCropClick
        )
        ToolbarIconButton(
            icon = Icons.Rounded.Brush,
            label = "Draw",
            selected = selectedTool == EditorTool.Draw,
            onClick = onDrawClick
        )
        ToolbarIconButton(
            icon = Icons.Rounded.TextFields,
            label = "Text",
            selected = false,
            onClick = onTextClick
        )
        ToolbarIconButton(
            icon = Icons.Rounded.AlternateEmail,
            label = "Mention",
            selected = false,
            onClick = onMentionClick
        )
    }
}

@Composable
private fun ToolbarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f),
        label = "toolbarButtonScale"
    )
    Surface(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.padding(10.dp).size(20.dp)
        )
    }
}

@Composable
private fun ExpressiveIconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    contentDescription: String,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.9f else 1f,
        animationSpec = spring(dampingRatio = 0.58f, stiffness = 430f),
        label = "topButtonScale"
    )
    Box(
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .semantics { this.contentDescription = contentDescription }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun InlineTextToolbar(
    textBold: Boolean,
    textItalic: Boolean,
    textBackgroundColor: Color,
    onBoldToggle: () -> Unit,
    onItalicToggle: () -> Unit,
    onColorClick: () -> Unit,
    onBackgroundClick: () -> Unit,
    onDone: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(selected = textBold, onClick = onBoldToggle, label = { Text("B") })
            FilterChip(selected = textItalic, onClick = onItalicToggle, label = { Text("I") })
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ColorSwatch(color = textBackgroundColor, onClick = onBackgroundClick)
            IconButton(onClick = onColorClick) {
                Icon(imageVector = Icons.Rounded.FormatColorText, contentDescription = "Text color")
            }
            ExpressiveIconButton(onClick = onDone, contentDescription = "Done editing text") {
                Icon(imageVector = Icons.Rounded.Done, contentDescription = null)
            }
        }
    }
}

@Composable
private fun MentionPickerDialog(
    suggestions: List<String>,
    onQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onAddMention: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(query) {
        onQueryChange(query.trim().removePrefix("@"))
    }
    val visibleSuggestions = remember(suggestions) {
        suggestions.distinct().take(6)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add mention") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.removePrefix("@") },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Username") },
                    singleLine = true
                )
                visibleSuggestions.forEach { username ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onAddMention(username) },
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "@$username",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (visibleSuggestions.isEmpty()) {
                    Text(
                        text = "No users found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAddMention(query.trim().removePrefix("@")) }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UnifiedColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
) {
    val palette = remember {
        listOf(
            Color.Transparent,
            Color.White,
            Color.Black,
            Color.Red,
            Color(0xFFFF9800),
            Color.Yellow,
            Color(0xFF8BC34A),
            Color.Green,
            Color(0xFF00BCD4),
            Color.Cyan,
            Color.Blue,
            Color(0xFF3F51B5),
            Color(0xFFE91E63),
            Color.Magenta
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    palette.forEach { color ->
                        ColorSwatch(
                            color = color,
                            selected = color == initialColor,
                            onClick = { onColorSelected(color) }
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(if (color.alpha == 0f) Color.Black.copy(alpha = 0.08f) else color)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.45f),
                shape = CircleShape
            )
            .clickable(onClick = onClick)
    ) {
        if (color.alpha == 0f) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Transparent",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(16.dp)
            )
        }
    }
}

private suspend fun exportEditedStoryImage(
    context: Context,
    source: CreatePostMedia,
    viewportSize: IntSize,
    zoom: Float,
    pan: Offset,
    drawStrokes: List<DrawStroke>,
    textOverlays: List<TextOverlay>,
    mentionOverlays: List<MentionOverlay>,
): CreatePostMedia? = withContext(Dispatchers.Default) {
    val bitmap = decodeBitmap(context, source.uri)?.let(::ensureSoftwareBitmap) ?: return@withContext null
    val targetWidth = DEFAULT_EXPORT_WIDTH
    val targetHeight = DEFAULT_EXPORT_HEIGHT
    val previewWidthPx = viewportSize.width.takeIf { it > 0 } ?: targetWidth
    val previewHeightPx = viewportSize.height.takeIf { it > 0 } ?: targetHeight
    val previewWidth = previewWidthPx.toFloat()
    val previewHeight = previewHeightPx.toFloat()

    // Render exactly what the editor shows at viewport resolution first.
    val previewComposite = Bitmap.createBitmap(previewWidthPx, previewHeightPx, Bitmap.Config.ARGB_8888)
    val previewCanvas = android.graphics.Canvas(previewComposite)
    drawBaseImageLikeEditor(
        canvas = previewCanvas,
        bitmap = bitmap,
        viewportWidth = previewWidth,
        viewportHeight = previewHeight,
        zoom = zoom,
        pan = pan
    )

    val strokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    drawStrokes.forEach { stroke ->
        if (stroke.points.size < 2) return@forEach
        strokePaint.color = stroke.color.toArgb()
        strokePaint.strokeWidth = stroke.widthPx
        val path = android.graphics.Path()
        val first = stroke.points.first()
        path.moveTo(first.x, first.y)
        for (i in 1 until stroke.points.size) {
            val prev = stroke.points[i - 1]
            val current = stroke.points[i]
            val midX = (prev.x + current.x) / 2f
            val midY = (prev.y + current.y) / 2f
            path.quadTo(
                prev.x,
                prev.y,
                midX,
                midY
            )
        }
        previewCanvas.drawPath(path, strokePaint)
    }

    val textPaint = Paint().apply {
        isAntiAlias = true
    }
    val textBgPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    textOverlays.forEach { overlay ->
        textPaint.color = overlay.color.toArgb()
        val fontPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            overlay.fontSizeSp,
            context.resources.displayMetrics
        )
        textPaint.textSize = fontPx
        textPaint.typeface = resolveStoryTextTypeface(
            context = context,
            isBold = overlay.isBold,
            isItalic = overlay.isItalic
        )
        val x = overlay.offset.x
        val y = overlay.offset.y
        drawWrappedTextOverlayToCanvas(
            canvas = previewCanvas,
            anchorX = x,
            anchorY = y,
            text = overlay.text,
            textPaint = textPaint,
            backgroundColor = overlay.backgroundColor,
            backgroundPaint = textBgPaint,
            boxWidthPx = overlay.boxWidthPx,
            horizontalPaddingPx = 8f,
            verticalPaddingPx = 4f,
            cornerRadiusPx = 10f
        )
    }

    val mentionPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        textSize = 34f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD)
    }
    val mentionBgPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#77000000")
    }

    mentionOverlays.forEach { mention ->
        val x = mention.offset.x
        val y = mention.offset.y
        drawMentionOverlayToCanvas(
            canvas = previewCanvas,
            anchorX = x,
            anchorY = y,
            username = mention.username,
            textPaint = mentionPaint,
            backgroundPaint = mentionBgPaint,
            horizontalPaddingPx = 10f,
            verticalPaddingPx = 6f,
            cornerRadiusPx = 14f
        )
    }

    val export = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(export)
    val dstRect = RectF(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat())
    canvas.drawBitmap(previewComposite, null, dstRect, null)

    val outFile = File(
        context.cacheDir,
        "story_editor_${UUID.randomUUID()}.jpg"
    )
    FileOutputStream(outFile).use { stream ->
        export.compress(Bitmap.CompressFormat.JPEG, 92, stream)
    }

    bitmap.recycle()
    previewComposite.recycle()
    export.recycle()

    CreatePostMedia(
        uri = Uri.fromFile(outFile),
        fileName = outFile.name,
        mimeType = "image/jpeg",
        sizeBytes = outFile.length(),
    )
}

@androidx.annotation.OptIn(UnstableApi::class)
private suspend fun exportEditedStoryVideo(
    context: Context,
    source: CreatePostMedia,
    viewportSize: IntSize,
    zoom: Float,
    pan: Offset,
    drawStrokes: List<DrawStroke>,
    textOverlays: List<TextOverlay>,
    mentionOverlays: List<MentionOverlay>,
    onProgress: (Int) -> Unit = {},
): CreatePostMedia? {
    val overlayBitmap = withContext(Dispatchers.Default) {
        renderOverlayBitmap(
            context = context,
            viewportSize = viewportSize,
            drawStrokes = drawStrokes,
            textOverlays = textOverlays,
            mentionOverlays = mentionOverlays
        )
    }

    val outFile = File(
        context.cacheDir,
        "story_editor_${UUID.randomUUID()}.mp4"
    )
    if (outFile.exists()) {
        outFile.delete()
    }

    val effects = mutableListOf<Effect>()
    buildCropEffect(
        viewportSize = viewportSize,
        zoom = zoom,
        pan = pan
    )?.let(effects::add)
    effects.add(
        Presentation.createForWidthAndHeight(
            DEFAULT_EXPORT_WIDTH,
            DEFAULT_EXPORT_HEIGHT,
            Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP
        )
    )
    effects.add(
        OverlayEffect(
            listOf(
                BitmapOverlay.createStaticBitmapOverlay(overlayBitmap)
            )
        )
    )

    val editedItem = EditedMediaItem.Builder(
        MediaItem.fromUri(source.uri)
    ).setEffects(
        Effects(
            emptyList(),
            effects
        )
    ).build()

    val exported = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<Boolean> { continuation ->
            var progressJob: Job? = null
            val transformer = Transformer.Builder(context)
                .addListener(
                    object : Transformer.Listener {
                        override fun onCompleted(
                            composition: androidx.media3.transformer.Composition,
                            exportResult: ExportResult
                        ) {
                            onProgress(100)
                            progressJob?.cancel()
                            if (continuation.isActive) {
                                continuation.resume(true)
                            }
                        }

                        override fun onError(
                            composition: androidx.media3.transformer.Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            progressJob?.cancel()
                            if (continuation.isActive) {
                                continuation.resume(false)
                            }
                        }
                    }
                )
                .build()

            val progressHolder = androidx.media3.transformer.ProgressHolder()
            progressJob = launch {
                while (isActive) {
                    when (transformer.getProgress(progressHolder)) {
                        Transformer.PROGRESS_STATE_AVAILABLE -> {
                            onProgress(progressHolder.progress.coerceIn(0, 99))
                        }
                        Transformer.PROGRESS_STATE_WAITING_FOR_AVAILABILITY -> {
                            onProgress(0)
                        }
                    }
                    delay(120)
                }
            }

            continuation.invokeOnCancellation {
                progressJob?.cancel()
                transformer.cancel()
            }

            transformer.start(editedItem, outFile.absolutePath)
        }
    }

    if (!exported || !outFile.exists() || outFile.length() <= 0L) {
        if (outFile.exists()) {
            outFile.delete()
        }
        return null
    }

    return CreatePostMedia(
        uri = Uri.fromFile(outFile),
        fileName = outFile.name,
        mimeType = MimeTypes.VIDEO_MP4,
        sizeBytes = outFile.length(),
    )
}

private fun buildCropEffect(
    viewportSize: IntSize,
    zoom: Float,
    pan: Offset,
): Crop? {
    if (zoom <= 1.01f && kotlin.math.abs(pan.x) < 1f && kotlin.math.abs(pan.y) < 1f) {
        return null
    }
    val width = viewportSize.width.takeIf { it > 0 }?.toFloat() ?: DEFAULT_EXPORT_WIDTH.toFloat()
    val height = viewportSize.height.takeIf { it > 0 }?.toFloat() ?: DEFAULT_EXPORT_HEIGHT.toFloat()
    val tx = (2f * pan.x / width).coerceIn(-1f, 1f)
    val ty = (-2f * pan.y / height).coerceIn(-1f, 1f)
    val left = ((-1f - tx) / zoom).coerceIn(-1f, 0.9f)
    val right = ((1f - tx) / zoom).coerceIn(-0.9f, 1f)
    val top = ((1f - ty) / zoom).coerceIn(-0.9f, 1f)
    val bottom = ((-1f - ty) / zoom).coerceIn(-1f, 0.9f)
    if (right - left <= 0.1f || top - bottom <= 0.1f) {
        return null
    }
    return Crop(left, right, bottom, top)
}

private fun renderOverlayBitmap(
    context: Context,
    viewportSize: IntSize,
    drawStrokes: List<DrawStroke>,
    textOverlays: List<TextOverlay>,
    mentionOverlays: List<MentionOverlay>,
): Bitmap {
    val previewWidthPx = viewportSize.width.takeIf { it > 0 } ?: DEFAULT_EXPORT_WIDTH
    val previewHeightPx = viewportSize.height.takeIf { it > 0 } ?: DEFAULT_EXPORT_HEIGHT
    val previewOverlay = Bitmap.createBitmap(previewWidthPx, previewHeightPx, Bitmap.Config.ARGB_8888)
    val previewCanvas = android.graphics.Canvas(previewOverlay)

    val strokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    drawStrokes.forEach { stroke ->
        if (stroke.points.size < 2) return@forEach
        strokePaint.color = stroke.color.toArgb()
        strokePaint.strokeWidth = stroke.widthPx
        val path = android.graphics.Path()
        val first = stroke.points.first()
        path.moveTo(first.x, first.y)
        for (i in 1 until stroke.points.size) {
            val prev = stroke.points[i - 1]
            val current = stroke.points[i]
            val midX = (prev.x + current.x) / 2f
            val midY = (prev.y + current.y) / 2f
            path.quadTo(
                prev.x,
                prev.y,
                midX,
                midY
            )
        }
        previewCanvas.drawPath(path, strokePaint)
    }

    val textPaint = Paint().apply {
        isAntiAlias = true
    }
    val textBgPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    textOverlays.forEach { overlay ->
        textPaint.color = overlay.color.toArgb()
        val fontPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            overlay.fontSizeSp,
            context.resources.displayMetrics
        )
        textPaint.textSize = fontPx
        textPaint.typeface = resolveStoryTextTypeface(
            context = context,
            isBold = overlay.isBold,
            isItalic = overlay.isItalic
        )
        val x = overlay.offset.x
        val y = overlay.offset.y
        drawWrappedTextOverlayToCanvas(
            canvas = previewCanvas,
            anchorX = x,
            anchorY = y,
            text = overlay.text,
            textPaint = textPaint,
            backgroundColor = overlay.backgroundColor,
            backgroundPaint = textBgPaint,
            boxWidthPx = overlay.boxWidthPx,
            horizontalPaddingPx = 8f,
            verticalPaddingPx = 4f,
            cornerRadiusPx = 10f
        )
    }

    val mentionPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.WHITE
        textSize = 34f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD)
    }
    val mentionBgPaint = Paint().apply {
        isAntiAlias = true
        color = android.graphics.Color.parseColor("#77000000")
    }

    mentionOverlays.forEach { mention ->
        val x = mention.offset.x
        val y = mention.offset.y
        drawMentionOverlayToCanvas(
            canvas = previewCanvas,
            anchorX = x,
            anchorY = y,
            username = mention.username,
            textPaint = mentionPaint,
            backgroundPaint = mentionBgPaint,
            horizontalPaddingPx = 10f,
            verticalPaddingPx = 6f,
            cornerRadiusPx = 14f
        )
    }

    val export = Bitmap.createBitmap(DEFAULT_EXPORT_WIDTH, DEFAULT_EXPORT_HEIGHT, Bitmap.Config.ARGB_8888)
    val exportCanvas = android.graphics.Canvas(export)
    val dstRect = RectF(0f, 0f, DEFAULT_EXPORT_WIDTH.toFloat(), DEFAULT_EXPORT_HEIGHT.toFloat())
    exportCanvas.drawBitmap(previewOverlay, null, dstRect, null)
    previewOverlay.recycle()

    return export
}

private fun drawWrappedTextOverlayToCanvas(
    canvas: android.graphics.Canvas,
    anchorX: Float,
    anchorY: Float,
    text: String,
    textPaint: Paint,
    backgroundColor: Color,
    backgroundPaint: Paint,
    boxWidthPx: Float,
    horizontalPaddingPx: Float,
    verticalPaddingPx: Float,
    cornerRadiusPx: Float,
    maxLines: Int = 4,
) {
    if (text.isBlank()) return
    val metrics = buildWrappedTextLayoutMetrics(
        text = text,
        textPaint = textPaint,
        boxWidthPx = boxWidthPx,
        maxLines = maxLines
    )
    if (backgroundColor.alpha > 0f) {
        backgroundPaint.color = backgroundColor.toArgb()
        val rect = RectF(
            anchorX,
            anchorY,
            anchorX + metrics.contentWidth + (horizontalPaddingPx * 2f),
            anchorY + metrics.layout.height + (verticalPaddingPx * 2f)
        )
        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, backgroundPaint)
    }
    canvas.save()
    canvas.translate(
        anchorX + horizontalPaddingPx - metrics.minLineLeft,
        anchorY + verticalPaddingPx
    )
    metrics.layout.draw(canvas)
    canvas.restore()
}

private data class WrappedTextLayoutMetrics(
    val layout: StaticLayout,
    val minLineLeft: Float,
    val contentWidth: Float,
)

private fun buildWrappedTextLayoutMetrics(
    text: String,
    textPaint: Paint,
    boxWidthPx: Float,
    maxLines: Int,
): WrappedTextLayoutMetrics {
    val safeText = text.ifBlank { " " }
    val layoutWidth = boxWidthPx.roundToInt().coerceAtLeast(1)
    val layout = StaticLayout.Builder
        .obtain(safeText, 0, safeText.length, TextPaint(textPaint), layoutWidth)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setIncludePad(false)
        .setMaxLines(maxLines.coerceAtLeast(1))
        .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
        .build()
    val minLineLeft = (0 until layout.lineCount)
        .minOfOrNull { line -> layout.getLineLeft(line) } ?: 0f
    val maxLineRight = (0 until layout.lineCount)
        .maxOfOrNull { line -> layout.getLineRight(line) } ?: 0f
    return WrappedTextLayoutMetrics(
        layout = layout,
        minLineLeft = minLineLeft,
        contentWidth = (maxLineRight - minLineLeft).coerceAtLeast(0f)
    )
}

private fun resolveStoryTextTypeface(
    context: Context,
    isBold: Boolean,
    isItalic: Boolean,
): android.graphics.Typeface {
    val configuration = context.resources.configuration
    val language = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        configuration.locales[0]?.language
    } else {
        @Suppress("DEPRECATION")
        configuration.locale?.language
    }.orEmpty()

    val isFarsi = language.equals("fa", ignoreCase = true)
    val normalTypeface = if (isFarsi) {
        ResourcesCompat.getFont(context, R.font.shabnam) ?: android.graphics.Typeface.DEFAULT
    } else {
        android.graphics.Typeface.DEFAULT
    }
    val boldTypeface = if (isFarsi) {
        ResourcesCompat.getFont(context, R.font.shabnam_bold) ?: normalTypeface
    } else {
        android.graphics.Typeface.DEFAULT_BOLD
    }

    val baseTypeface = if (isBold) boldTypeface else normalTypeface
    return when {
        isBold && isItalic -> android.graphics.Typeface.create(baseTypeface, android.graphics.Typeface.BOLD_ITALIC)
        isItalic -> android.graphics.Typeface.create(baseTypeface, android.graphics.Typeface.ITALIC)
        isBold -> boldTypeface
        else -> baseTypeface
    }
}

private fun drawBaseImageLikeEditor(
    canvas: android.graphics.Canvas,
    bitmap: Bitmap,
    viewportWidth: Float,
    viewportHeight: Float,
    zoom: Float,
    pan: Offset,
) {
    val blurredBackground = createApproxBlurBitmap(bitmap)
    drawBitmapWithEditorTransform(
        canvas = canvas,
        bitmap = blurredBackground,
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight,
        zoom = 1f,
        pan = Offset.Zero,
        scaleToFill = true
    )
    canvas.drawRect(
        0f,
        0f,
        viewportWidth,
        viewportHeight,
        Paint().apply { color = android.graphics.Color.argb(46, 0, 0, 0) }
    )
    drawBitmapWithEditorTransform(
        canvas = canvas,
        bitmap = bitmap,
        viewportWidth = viewportWidth,
        viewportHeight = viewportHeight,
        zoom = zoom,
        pan = pan,
        scaleToFill = false
    )
    if (blurredBackground !== bitmap && !blurredBackground.isRecycled) {
        blurredBackground.recycle()
    }
}

private fun drawBitmapWithEditorTransform(
    canvas: android.graphics.Canvas,
    bitmap: Bitmap,
    viewportWidth: Float,
    viewportHeight: Float,
    zoom: Float,
    pan: Offset,
    scaleToFill: Boolean,
) {
    val baseScale = if (scaleToFill) {
        max(viewportWidth / bitmap.width, viewportHeight / bitmap.height)
    } else {
        min(viewportWidth / bitmap.width, viewportHeight / bitmap.height)
    }
    val baseTx = (viewportWidth - bitmap.width * baseScale) / 2f
    val baseTy = (viewportHeight - bitmap.height * baseScale) / 2f
    val centerX = viewportWidth / 2f
    val centerY = viewportHeight / 2f

    val matrix = android.graphics.Matrix().apply {
        postScale(baseScale, baseScale)
        postTranslate(baseTx, baseTy)
        postTranslate(-centerX, -centerY)
        postScale(zoom, zoom)
        postTranslate(centerX, centerY)
        postTranslate(pan.x, pan.y)
    }
    canvas.drawBitmap(bitmap, matrix, null)
}

private fun createApproxBlurBitmap(bitmap: Bitmap): Bitmap {
    val longEdge = max(bitmap.width, bitmap.height)
    val targetLongEdge = 540
    val scale = if (longEdge > targetLongEdge) {
        targetLongEdge.toFloat() / longEdge.toFloat()
    } else {
        1f
    }
    val sampleWidth = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
    val sampleHeight = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
    val sampled = Bitmap.createScaledBitmap(bitmap, sampleWidth, sampleHeight, true)
    val radius = (max(sampleWidth, sampleHeight) / 40).coerceIn(6, 14)
    val blurredSample = boxBlurBitmap(sampled, radius = radius, passes = 3)
    if (sampled !== bitmap && sampled !== blurredSample && !sampled.isRecycled) {
        sampled.recycle()
    }
    val blurredFull = Bitmap.createScaledBitmap(
        blurredSample,
        bitmap.width,
        bitmap.height,
        true
    )
    if (blurredFull !== blurredSample && !blurredSample.isRecycled) {
        blurredSample.recycle()
    }
    return blurredFull
}

private fun boxBlurBitmap(
    source: Bitmap,
    radius: Int,
    passes: Int,
): Bitmap {
    if (radius <= 0 || passes <= 0) return source.copy(Bitmap.Config.ARGB_8888, false)
    val width = source.width
    val height = source.height
    val count = width * height
    val src = IntArray(count)
    val tmp = IntArray(count)
    source.getPixels(src, 0, width, 0, 0, width, height)

    repeat(passes) {
        boxBlurHorizontal(src, tmp, width, height, radius)
        boxBlurVertical(tmp, src, width, height, radius)
    }

    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { result ->
        result.setPixels(src, 0, width, 0, 0, width, height)
    }
}

private fun boxBlurHorizontal(
    src: IntArray,
    dst: IntArray,
    width: Int,
    height: Int,
    radius: Int,
) {
    val kernelSize = radius * 2 + 1
    for (y in 0 until height) {
        val rowStart = y * width
        var sumA = 0
        var sumR = 0
        var sumG = 0
        var sumB = 0

        for (i in -radius..radius) {
            val x = i.coerceIn(0, width - 1)
            val color = src[rowStart + x]
            sumA += (color ushr 24) and 0xFF
            sumR += (color ushr 16) and 0xFF
            sumG += (color ushr 8) and 0xFF
            sumB += color and 0xFF
        }

        for (x in 0 until width) {
            dst[rowStart + x] = ((sumA / kernelSize) shl 24) or
                ((sumR / kernelSize) shl 16) or
                ((sumG / kernelSize) shl 8) or
                (sumB / kernelSize)

            val removeX = (x - radius).coerceIn(0, width - 1)
            val addX = (x + radius + 1).coerceIn(0, width - 1)
            val removeColor = src[rowStart + removeX]
            val addColor = src[rowStart + addX]

            sumA += ((addColor ushr 24) and 0xFF) - ((removeColor ushr 24) and 0xFF)
            sumR += ((addColor ushr 16) and 0xFF) - ((removeColor ushr 16) and 0xFF)
            sumG += ((addColor ushr 8) and 0xFF) - ((removeColor ushr 8) and 0xFF)
            sumB += (addColor and 0xFF) - (removeColor and 0xFF)
        }
    }
}

private fun boxBlurVertical(
    src: IntArray,
    dst: IntArray,
    width: Int,
    height: Int,
    radius: Int,
) {
    val kernelSize = radius * 2 + 1
    for (x in 0 until width) {
        var sumA = 0
        var sumR = 0
        var sumG = 0
        var sumB = 0

        for (i in -radius..radius) {
            val y = i.coerceIn(0, height - 1)
            val color = src[y * width + x]
            sumA += (color ushr 24) and 0xFF
            sumR += (color ushr 16) and 0xFF
            sumG += (color ushr 8) and 0xFF
            sumB += color and 0xFF
        }

        for (y in 0 until height) {
            dst[y * width + x] = ((sumA / kernelSize) shl 24) or
                ((sumR / kernelSize) shl 16) or
                ((sumG / kernelSize) shl 8) or
                (sumB / kernelSize)

            val removeY = (y - radius).coerceIn(0, height - 1)
            val addY = (y + radius + 1).coerceIn(0, height - 1)
            val removeColor = src[removeY * width + x]
            val addColor = src[addY * width + x]

            sumA += ((addColor ushr 24) and 0xFF) - ((removeColor ushr 24) and 0xFF)
            sumR += ((addColor ushr 16) and 0xFF) - ((removeColor ushr 16) and 0xFF)
            sumG += ((addColor ushr 8) and 0xFF) - ((removeColor ushr 8) and 0xFF)
            sumB += (addColor and 0xFF) - (removeColor and 0xFF)
        }
    }
}

private fun drawMentionOverlayToCanvas(
    canvas: android.graphics.Canvas,
    anchorX: Float,
    anchorY: Float,
    username: String,
    textPaint: Paint,
    backgroundPaint: Paint,
    horizontalPaddingPx: Float,
    verticalPaddingPx: Float,
    cornerRadiusPx: Float,
) {
    val text = "@$username"
    val textWidth = textPaint.measureText(text)
    val metrics = textPaint.fontMetrics
    val textHeight = metrics.descent - metrics.ascent
    val rect = RectF(
        anchorX,
        anchorY,
        anchorX + textWidth + (horizontalPaddingPx * 2f),
        anchorY + textHeight + (verticalPaddingPx * 2f)
    )
    canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, backgroundPaint)
    val baseline = anchorY + verticalPaddingPx - metrics.ascent
    canvas.drawText(text, anchorX + horizontalPaddingPx, baseline, textPaint)
}

private fun decodeBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }
    } catch (_: Exception) {
        null
    }
}

private fun ensureSoftwareBitmap(bitmap: Bitmap): Bitmap {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) {
        return bitmap.copy(Bitmap.Config.ARGB_8888, false)
    }
    return bitmap
}

private fun colorToHex(color: Color): String {
    val argb = color.toArgb()
    return String.format("#%08X", argb)
}

private fun parseColorFromHex(input: String): Color? {
    return try {
        Color(android.graphics.Color.parseColor(input.ifBlank { "#FFFFFFFF" }))
    } catch (_: Exception) {
        null
    }
}

private fun normalizeHexInput(raw: String): String {
    var cleaned = raw.trim().uppercase(Locale.US)
    if (!cleaned.startsWith("#")) cleaned = "#$cleaned"
    return cleaned.take(9)
}

private fun Modifier.offsetFromCenter(centerPoint: Offset): Modifier {
    return this.then(
        Modifier.graphicsLayer {
            translationX = centerPoint.x
            translationY = centerPoint.y
        }
    )
}
