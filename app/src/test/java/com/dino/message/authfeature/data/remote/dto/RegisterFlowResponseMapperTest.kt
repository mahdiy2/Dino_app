package com.dino.message.authfeature.data.remote.dto

import org.junit.Assert.assertEquals
import org.junit.Test

class RegisterFlowResponseMapperTest {

    @Test
    fun `register start response maps registration id and expiry`() {
        val mapped = RegisterStartResponse(
            message = "Registration pending confirmation",
            registration_id = "reg_123",
            expires_in_seconds = 1800
        ).mapResponseToResult()

        assertEquals("reg_123", mapped.registrationId)
        assertEquals(1800, mapped.expiresInSeconds)
    }

    @Test(expected = Exception::class)
    fun `register start response throws when registration id is blank`() {
        RegisterStartResponse(
            message = "Registration pending confirmation",
            registration_id = "  ",
            expires_in_seconds = 1800
        ).mapResponseToResult()
    }

    @Test
    fun `register confirm response maps success message`() {
        RegisterConfirmResponse(message = "User registered").mapResponseToResult()
    }

    @Test(expected = Exception::class)
    fun `register confirm response throws for non success message`() {
        RegisterConfirmResponse(message = "something went wrong").mapResponseToResult()
    }
}
