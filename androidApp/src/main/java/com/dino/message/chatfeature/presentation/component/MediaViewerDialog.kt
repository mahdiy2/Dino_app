package com.dino.message.chatfeature.presentation.component

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dino.message.corefeature.presentation.platform.PlatformAsyncImage
import com.dino.message.corefeature.presentation.platform.PlatformImageRequestOptions
import com.dino.message.corefeature.presentation.platform.PlatformVideoPlayer
import com.dino.message.corefeature.presentation.platform.rememberPlatformImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import com.dino.message.corefeature.presentation.icons.AppIcons

@Composable
fun MediaViewerDialog(
    url: String,
    isVideo: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (isVideo) {
                PlatformVideoPlayer(
                    url = url,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                PlatformAsyncImage(
                    model = rememberPlatformImageRequest(
                        data = url,
                        options = PlatformImageRequestOptions(crossfade = true)
                    ),
                    contentDescription = "Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        scope.launch {
                            saveMediaToGallery(context, url, isVideo)
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        imageVector = AppIcons.Download,
                        contentDescription = "Save to gallery",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        imageVector = AppIcons.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

private suspend fun saveMediaToGallery(
    context: android.content.Context,
    url: String,
    isVideo: Boolean
) {
    try {
        val bytes = withContext(Dispatchers.IO) {
            URL(url).openStream().use { it.readBytes() }
        }

        val fileName = if (isVideo)
            "VID_${System.currentTimeMillis()}.mp4"
        else
            "IMG_${System.currentTimeMillis()}.jpg"

        val mimeType = if (isVideo) "video/mp4" else "image/jpeg"

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val relativePath = if (isVideo)
                    Environment.DIRECTORY_MOVIES + "/SecMessage"
                else
                    Environment.DIRECTORY_PICTURES + "/SecMessage"
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = if (isVideo)
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val resolver = context.contentResolver
        val uri = resolver.insert(collection, contentValues)

        if (uri != null) {
            withContext(Dispatchers.IO) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(bytes)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Saved to gallery", Toast.LENGTH_SHORT).show()
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to save", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
