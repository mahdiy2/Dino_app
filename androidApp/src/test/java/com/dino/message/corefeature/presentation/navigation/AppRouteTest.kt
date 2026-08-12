package com.dino.message.corefeature.presentation.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRouteTest {
    @Test
    fun modalRoutesAreExplicitlyMarked() {
        assertTrue(AppRoute.PlaylistSheet.isModalRoute())
        assertTrue(AppRoute.ThreadCommentsSheet(postId = 42L).isModalRoute())
        assertTrue(AppRoute.PrivateKeyResetDialog.isModalRoute())
    }

    @Test
    fun screenRoutesAreNotModalRoutes() {
        assertFalse(AppRoute.Main.isModalRoute())
        assertFalse(AppRoute.Chat(chatId = "user").isModalRoute())
        assertFalse(AppRoute.SharedPostDetails(postId = 42L).isModalRoute())
    }
}
