package com.dino.message.authfeature.domain.usecase

import com.dino.message.authfeature.domain.model.LoginUser
import com.dino.message.authfeature.domain.model.RegisterConfirmUser
import com.dino.message.authfeature.domain.model.RegisterStartUser
import com.dino.message.authfeature.domain.model.RegisterUser
import com.dino.message.authfeature.domain.repository.AuthRepository
import com.dino.message.corefeature.domain.model.StringResourceContent
import com.dino.message.corefeature.domain.model.asStringResourceContent
import com.dino.message.corefeature.presentation.util.Resource
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterUseCaseTest {

    @Test
    fun `execute emits loading then success`() = runBlocking {
        val fakeRepository = FakeAuthRepository().apply {
            registerStartResult = Resource.Success(
                RegisterStartUser(registrationId = "reg_123", expiresInSeconds = 1800)
            )
        }
        val useCase = RegisterUseCase(fakeRepository)

        val emissions = useCase.execute(
            username = "alice",
            pass = "strong_password",
            publicKey = "public_key_value",
            clientNonce = "nonce_abc"
        ).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is Resource.Loading)
        assertTrue(emissions[1] is Resource.Success)
        val success = emissions[1] as Resource.Success
        assertEquals("reg_123", (success.data as RegisterStartUser).registrationId)
        assertEquals("nonce_abc", fakeRepository.lastClientNonce)
    }

    @Test
    fun `execute emits loading then error`() = runBlocking {
        val fakeRepository = FakeAuthRepository().apply {
            registerStartResult = Resource.Error("Username already exists".asStringResourceContent())
        }
        val useCase = RegisterUseCase(fakeRepository)

        val emissions = useCase.execute(
            username = "alice",
            pass = "strong_password",
            publicKey = "public_key_value",
            clientNonce = "nonce_abc"
        ).toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is Resource.Loading)
        assertTrue(emissions[1] is Resource.Error)
        val error = emissions[1] as Resource.Error
        val content = error.message as StringResourceContent.Str
        assertEquals("Username already exists", content.data)
    }

    private class FakeAuthRepository : AuthRepository {
        var registerStartResult: Resource<RegisterStartUser> =
            Resource.Success(RegisterStartUser("default", 1800))
        var lastClientNonce: String? = null

        override suspend fun register(
            userName: String,
            pass: String,
            publicKey: String
        ): Resource<RegisterUser> = Resource.Success(RegisterUser())

        override suspend fun registerStart(
            userName: String,
            pass: String,
            publicKey: String,
            clientNonce: String
        ): Resource<RegisterStartUser> {
            lastClientNonce = clientNonce
            return registerStartResult
        }

        override suspend fun registerConfirm(registrationId: String): Resource<RegisterConfirmUser> {
            return Resource.Success(RegisterConfirmUser())
        }

        override suspend fun login(userName: String, pass: String): Resource<LoginUser> {
            return Resource.Success(LoginUser("", ""))
        }
    }
}
