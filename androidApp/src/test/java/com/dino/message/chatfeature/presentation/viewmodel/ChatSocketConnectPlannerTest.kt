package com.dino.message.chatfeature.presentation.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatSocketConnectPlannerTest {

    @Test
    fun `already connected still registers listeners and runs sync`() {
        val plan = ChatSocketConnectPlanner.plan(
            forceReconnect = false,
            isConnected = true,
            isSocketConnecting = false,
        )

        assertTrue(plan.shouldRegisterListeners)
        assertTrue(plan.shouldRunConnectedSync)
        assertFalse(plan.shouldConnectSocket)
    }

    @Test
    fun `connecting state registers listeners without reconnect`() {
        val plan = ChatSocketConnectPlanner.plan(
            forceReconnect = false,
            isConnected = false,
            isSocketConnecting = true,
        )

        assertTrue(plan.shouldRegisterListeners)
        assertFalse(plan.shouldRunConnectedSync)
        assertFalse(plan.shouldConnectSocket)
    }

    @Test
    fun `force reconnect connects socket and skips connected sync`() {
        val plan = ChatSocketConnectPlanner.plan(
            forceReconnect = true,
            isConnected = true,
            isSocketConnecting = true,
        )

        assertTrue(plan.shouldRegisterListeners)
        assertFalse(plan.shouldRunConnectedSync)
        assertTrue(plan.shouldConnectSocket)
    }
}
