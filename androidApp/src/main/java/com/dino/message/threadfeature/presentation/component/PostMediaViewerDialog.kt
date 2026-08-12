package com.dino.message.threadfeature.presentation.component

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dino.message.R
import com.dino.message.corefeature.presentation.platform.PlatformImageRequestOptions
import com.dino.message.corefeature.presentation.platform.PlatformSubcomposeAsyncImage
import com.dino.message.corefeature.presentation.platform.PlatformVideoPlayer
import com.dino.message.corefeature.presentation.platform.rememberPlatformImageRequest
import com.dino.message.threadfeature.domain.model.PostMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import com.dino.message.corefeature.presentation.icons.AppIcons

@Composable
fun PostMediaViewerDialog(
    mediaItems: List<PostMedia>,
    initialIndex: Int,
    canDownloadMedia: Boolean,
    onDismiss: () -> Unit
) {
    if (mediaItems.isEmpty()) return

    val context = LocalContext.current
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, mediaItems.lastIndex),
        pageCount = { mediaItems.size }
    )
    val thumbnailListState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        thumbnailListState.animateScrollToItem(
            index = pagerState.currentPage,
            scrollOffset = 0
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                key = { mediaItems[it].id }
            ) { page ->
                val item = mediaItems[page]
                val isVideo = item.mime_type.startsWith("video/", ignoreCase = true)

                if (isVideo) {
                    FullScreenVideoPlayer(
                        url = item.url,
                        isCurrentPage = pagerState.currentPage == page,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    FullScreenImage(
                        url = item.url,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (canDownloadMedia) {
                    IconButton(
                        onClick = {
                            val currentItem = mediaItems[pagerState.currentPage]
                            scope.launch {
                                saveMediaToGallery(
                                    context = context,
                                    url = currentItem.url,
                                    isVideo = currentItem.mime_type.startsWith("video/", ignoreCase = true)
                                )
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            imageVector = AppIcons.Download,
                            contentDescription = stringResource(R.string.download_media),
                            tint = Color.White
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        imageVector = AppIcons.Close,
                        contentDescription = stringResource(R.string.close),
                        tint = Color.White
                    )
                }
            }

            Text(
                text = "${pagerState.currentPage + 1} / ${mediaItems.size}",
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
                    .background(
                        Color.Black.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )

            if (mediaItems.size > 1) {
                LazyRow(
                    state = thumbnailListState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(
                        6.dp,
                        Alignment.CenterHorizontally
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    itemsIndexed(
                        items = mediaItems,
                        key = { _, item -> item.id }
                    ) { index, item ->
                        val isActive = pagerState.currentPage == index
                        val borderWidth by animateDpAsState(
                            targetValue = if (isActive) 2.dp else 0.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "thumb-border"
                        )

                        ThumbnailItem(
                            item = item,
                            borderWidth = borderWidth,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThumbnailItem(
    item: PostMedia,
    borderWidth: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val isVideo = item.mime_type.startsWith("video/", ignoreCase = true)
    val modelData: Any = if (
        item.url.startsWith("content://", ignoreCase = true) ||
        item.url.startsWith("file://", ignoreCase = true)
    ) Uri.parse(item.url) else item.url

    PlatformSubcomposeAsyncImage(
        model = rememberPlatformImageRequest(
            data = modelData,
            options = PlatformImageRequestOptions(
                crossfade = true,
                videoFrameMillis = if (isVideo) 0L else null,
                sizePx = 120,
            )
        ),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(6.dp))
            .then(
                if (borderWidth > 0.dp) {
                    Modifier.border(
                        borderWidth,
                        Color.White,
                        RoundedCornerShape(6.dp)
                    )
                } else Modifier
            )
            .clickable(onClick = onClick)
    )
}

@Composable
private fun FullScreenImage(
    url: String,
    modifier: Modifier = Modifier
) {
    val modelData: Any = if (
        url.startsWith("content://", ignoreCase = true) ||
        url.startsWith("file://", ignoreCase = true)
    ) Uri.parse(url) else url

    PlatformSubcomposeAsyncImage(
        model = rememberPlatformImageRequest(
            data = modelData,
            options = PlatformImageRequestOptions(crossfade = true)
        ),
        contentDescription = "Full-screen image",
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}

@Composable
private fun FullScreenVideoPlayer(
    url: String,
    isCurrentPage: Boolean,
    modifier: Modifier = Modifier
) {
    PlatformVideoPlayer(
        url = url,
        modifier = modifier,
        useController = true,
        playWhenReady = isCurrentPage,
        isPaused = !isCurrentPage,
        pauseCompetingAudio = isCurrentPage
    )
}

private suspend fun saveMediaToGallery(
    context: Context,
    url: String,
    isVideo: Boolean
) {
    try {
        val bytes = withContext(Dispatchers.IO) {
            when {
                url.startsWith("content://", ignoreCase = true) -> {
                    val uri = Uri.parse(url)
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                url.startsWith("file://", ignoreCase = true) -> {
                    val uri = Uri.parse(url)
                    uri.path?.let { path -> java.io.File(path).readBytes() }
                }
                else -> {
                    URL(url).openStream().use { it.readBytes() }
                }
            }
        } ?: run {
            Toast.makeText(context, context.getString(R.string.save_failed), Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = if (isVideo) {
            "VID_${System.currentTimeMillis()}.mp4"
        } else {
            "IMG_${System.currentTimeMillis()}.jpg"
        }
        val mimeType = if (isVideo) "video/mp4" else "image/jpeg"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val relativePath = if (isVideo) {
                    Environment.DIRECTORY_MOVIES + "/SecMessage"
                } else {
                    Environment.DIRECTORY_PICTURES + "/SecMessage"
                }
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = if (isVideo) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(collection, contentValues)
        if (uri == null) {
            Toast.makeText(context, context.getString(R.string.save_failed), Toast.LENGTH_SHORT).show()
            return
        }

        val writeSucceeded = withContext(Dispatchers.IO) {
            resolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(bytes)
            } != null
        }

        if (!writeSucceeded) {
            resolver.delete(uri, null, null)
            Toast.makeText(context, context.getString(R.string.save_failed), Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }

        Toast.makeText(context, context.getString(R.string.saved_to_gallery), Toast.LENGTH_SHORT).show()
    } catch (_: Exception) {
        Toast.makeText(context, context.getString(R.string.save_failed), Toast.LENGTH_SHORT).show()
    }
}
