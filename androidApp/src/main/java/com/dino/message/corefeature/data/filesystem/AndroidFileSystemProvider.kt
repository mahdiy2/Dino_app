package com.dino.message.corefeature.data.filesystem

import android.content.Context
import com.dino.message.corefeature.domain.filesystem.FileSystemProvider
import java.io.File

class AndroidFileSystemProvider(
    private val context: Context,
) : FileSystemProvider {
    override fun clearAppCacheDirectories() {
        clearDirectoryChildren(context.cacheDir)
        clearDirectoryChildren(context.externalCacheDir)
        clearDirectoryChildren(context.codeCacheDir)
    }

    private fun clearDirectoryChildren(directory: File?) {
        val files = directory?.listFiles() ?: return
        files.forEach { file ->
            file.deleteRecursively()
        }
    }
}
