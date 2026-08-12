package com.dino.message.chatfeature.presentation.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.ArrayDeque

class ClientMessageCorrelationResolverTest {

    @Test
    fun consumePending_resolvesByClientMessageId_evenWhenAcksArriveOutOfOrder() {
        val pendingMap = linkedMapOf(
            "id-1" to "first",
            "id-2" to "second",
        )
        val pendingOrder = ArrayDeque(listOf("id-1", "id-2"))

        val resolved = ClientMessageCorrelationResolver.consumePending(
            pendingByClientId = pendingMap,
            pendingOrder = pendingOrder,
            clientMessageId = "id-2",
        )

        assertEquals("id-2", resolved?.clientMessageId)
        assertEquals("second", resolved?.item)
        assertEquals(setOf("id-1"), pendingMap.keys)
        assertEquals(listOf("id-1"), pendingOrder.toList())
    }

    @Test
    fun consumePending_doesNotFallbackWhenProvidedClientMessageIdIsUnknown() {
        val pendingMap = linkedMapOf(
            "id-1" to "first",
            "id-2" to "second",
        )
        val pendingOrder = ArrayDeque(listOf("id-1", "id-2"))

        val resolved = ClientMessageCorrelationResolver.consumePending(
            pendingByClientId = pendingMap,
            pendingOrder = pendingOrder,
            clientMessageId = "id-unknown",
        )

        assertNull(resolved)
        assertEquals(setOf("id-1", "id-2"), pendingMap.keys)
        assertEquals(listOf("id-1", "id-2"), pendingOrder.toList())
    }

    @Test
    fun consumePending_fallsBackToQueueOrderWhenClientMessageIdIsMissing() {
        val pendingMap = linkedMapOf(
            "id-1" to "first",
            "id-2" to "second",
        )
        val pendingOrder = ArrayDeque(listOf("id-1", "id-2"))

        val resolved = ClientMessageCorrelationResolver.consumePending(
            pendingByClientId = pendingMap,
            pendingOrder = pendingOrder,
            clientMessageId = null,
        )

        assertEquals("id-1", resolved?.clientMessageId)
        assertEquals("first", resolved?.item)
        assertEquals(setOf("id-2"), pendingMap.keys)
        assertTrue(pendingOrder.size == 1 && pendingOrder.first == "id-2")
    }
}
