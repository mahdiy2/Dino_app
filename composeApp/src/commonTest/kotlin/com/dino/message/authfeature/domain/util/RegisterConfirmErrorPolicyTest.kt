package com.dino.message.authfeature.domain.util

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class RegisterConfirmErrorPolicyTest {

    @Test
    fun `should clear pending for expired session`() {
        assertTrue(RegisterConfirmErrorPolicy.shouldClearPendingSession("Registration session expired"))
    }

    @Test
    fun `should clear pending for not found session`() {
        assertTrue(RegisterConfirmErrorPolicy.shouldClearPendingSession("Registration session not found"))
    }

    @Test
    fun `should clear pending for username already exists conflict`() {
        assertTrue(RegisterConfirmErrorPolicy.shouldClearPendingSession("Username already exists"))
    }

    @Test
    fun `should not clear pending for transient errors`() {
        assertFalse(RegisterConfirmErrorPolicy.shouldClearPendingSession("Network timeout"))
        assertFalse(RegisterConfirmErrorPolicy.shouldClearPendingSession("503 Service unavailable"))
        assertFalse(RegisterConfirmErrorPolicy.shouldClearPendingSession("Unknown error"))
    }
}
