package com.dino.message.chatfeature.presentation.component

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

enum class MediaAccessLevel {
    FULL,
    LIMITED,
    DENIED
}

object MediaPermissionHandler {

    fun getRequiredPermissions(): Array<String> {
        return when {
            Build.VERSION.SDK_INT >= 34 -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            Build.VERSION.SDK_INT >= 33 -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
            else -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    fun checkAccessLevel(context: Context): MediaAccessLevel {
        return when {
            Build.VERSION.SDK_INT >= 34 -> {
                val hasImages = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
                val hasVideo = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_MEDIA_VIDEO
                ) == PackageManager.PERMISSION_GRANTED
                val hasPartial = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                ) == PackageManager.PERMISSION_GRANTED

                when {
                    hasImages && hasVideo -> MediaAccessLevel.FULL
                    hasPartial -> MediaAccessLevel.LIMITED
                    else -> MediaAccessLevel.DENIED
                }
            }
            Build.VERSION.SDK_INT >= 33 -> {
                val hasImages = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
                val hasVideo = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_MEDIA_VIDEO
                ) == PackageManager.PERMISSION_GRANTED

                if (hasImages && hasVideo) MediaAccessLevel.FULL else MediaAccessLevel.DENIED
            }
            else -> {
                val hasStorage = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED

                if (hasStorage) MediaAccessLevel.FULL else MediaAccessLevel.DENIED
            }
        }
    }

    fun resolveAccessLevel(permissionResults: Map<String, Boolean>): MediaAccessLevel {
        return when {
            Build.VERSION.SDK_INT >= 34 -> {
                val hasImages = permissionResults[Manifest.permission.READ_MEDIA_IMAGES] == true
                val hasVideo = permissionResults[Manifest.permission.READ_MEDIA_VIDEO] == true
                val hasPartial = permissionResults[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true

                when {
                    hasImages && hasVideo -> MediaAccessLevel.FULL
                    hasPartial -> MediaAccessLevel.LIMITED
                    else -> MediaAccessLevel.DENIED
                }
            }
            Build.VERSION.SDK_INT >= 33 -> {
                val hasImages = permissionResults[Manifest.permission.READ_MEDIA_IMAGES] == true
                val hasVideo = permissionResults[Manifest.permission.READ_MEDIA_VIDEO] == true

                if (hasImages && hasVideo) MediaAccessLevel.FULL else MediaAccessLevel.DENIED
            }
            else -> {
                val hasStorage = permissionResults[Manifest.permission.READ_EXTERNAL_STORAGE] == true

                if (hasStorage) MediaAccessLevel.FULL else MediaAccessLevel.DENIED
            }
        }
    }
}
