package com.dino.message.corefeature.presentation.navigation

fun AppRoute.isModalRoute(): Boolean = when (this) {
    is AppRoute.PlaylistSheet,
    is AppRoute.CreatePostOptionsSheet,
    is AppRoute.ThreadCommentsSheet,
    is AppRoute.SharePostSheet,
    is AppRoute.ReportPostSheet,
    is AppRoute.DeletePostSheet,
    is AppRoute.PrivateKeyResetDialog,
    is AppRoute.TrackPlaylistAddersDialog -> true
    else -> false
}
