package com.dino.message.corefeature.data.upload

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.dino.message.corefeature.domain.upload.ReadableMediaFile
import com.dino.message.corefeature.domain.upload.UploadMediaPicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun rememberAndroidUploadMediaPicker(): UploadMediaPicker {
    val resolver = LocalContext.current.applicationContext.contentResolver
    val provider = remember(resolver) { AndroidUploadContentProvider(resolver) }
    val scope = rememberCoroutineScope()
    val pending = remember { arrayOfNulls<(Result<ReadableMediaFile?>) -> Unit>(1) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val onResult = pending[0] ?: return@rememberLauncherForActivityResult
        if (uri == null) {
            pending[0] = null
            onResult(Result.success(null))
        } else {
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val file = provider.readableMediaFile(uri)
                        val metadata = AndroidMediaMetadataExtractor(resolver)
                            .extractWithThumbnail(uri, file.handle)
                        file.copy(metadata = metadata)
                    }
                }
                pending[0] = null
                onResult(result.mapCatching { file ->
                    file.copy(release = PersistedMediaGrants.acquire(resolver, uri))
                })
            }
        }
    }
    return remember(launcher) {
        UploadMediaPicker { categories, onResult ->
            check(pending[0] == null) { "A media selection is already pending" }
            pending[0] = onResult
            try {
                launcher.launch(categories.map { "${it.mimePrefix}/*" }.toTypedArray())
            } catch (error: Exception) {
                pending[0] = null
                onResult(Result.failure(error))
            }
        }
    }
}

// The same document can be retained by more than one draft.
private object PersistedMediaGrants {
    private val readers = mutableMapOf<Uri, Int>()
    private const val readFlag = Intent.FLAG_GRANT_READ_URI_PERMISSION

    @Synchronized
    fun acquire(resolver: ContentResolver, uri: Uri): () -> Unit {
        val count = readers[uri]
        if (count == null) {
            val alreadyPersisted = resolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
            if (alreadyPersisted) return {}
            if (runCatching { resolver.takePersistableUriPermission(uri, readFlag) }.isFailure) return {}
        }
        readers[uri] = (count ?: 0) + 1
        var released = false
        return {
            synchronized(this) {
                if (!released) {
                    released = true
                    val remaining = readers.getValue(uri) - 1
                    if (remaining == 0) {
                        readers.remove(uri)
                        runCatching { resolver.releasePersistableUriPermission(uri, readFlag) }
                    } else {
                        readers[uri] = remaining
                    }
                }
            }
        }
    }
}
