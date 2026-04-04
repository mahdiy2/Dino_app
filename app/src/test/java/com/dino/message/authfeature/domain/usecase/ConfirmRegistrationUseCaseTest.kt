package com.dino.message.authfeature.domain.usecase

import com.dino.message.authfeature.domain.model.LoginUser
import com.dino.message.authfeature.domain.model.RegisterConfirmUser
import com.dino.message.authfeature.domain.model.RegisterStartUser
import com.dino.message.authfeature.domain.model.RegisterUser
import com.dino.message.authfeature.domain.repository.AuthRepository
import com.dino.message.corefeature.domain.model.asStringResourceContent
import com.dino.message.corefeature.presentation.util.Resource
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfirmRegistrationUseCaseTest {

    @Test
    fun `execute emits loading then success`() = runBlocking {
        val fakeRepository = FakeAuthRepository().apply {
            confirmResult = Resource.Success(RegisterConfirmUser())
        }
        val useCase = ConfirmRegistrationUseCase(fakeRepository)

        val emissions = useCase.execute("reg_123").toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is Resource.Loading)
        assertTrue(emissions[1] is Resource.Success)
        assertEquals("reg_123", fakeRepository.lastRegistrationId)
    }

    @Test
    fun `execute emits loading then error`() = runBlocking {
        val fakeRepository = FakeAuthRepository().apply {
            confirmResult = Resource.Error("Registration session expired".asStringResourceContent())
        }
        val useCase = ConfirmRegistrationUseCase(fakeRepository)

        val emissions = useCase.execute("reg_123").toList()

        assertEquals(2, emissions.size)
        assertTrue(emissions[0] is Resource.Loading)
        assertTrue(emissions[1] is Resource.Error)
    }

    private class FakeAuthRepository : AuthRepository {
        var confirmResult: Resource<RegisterConfirmUser> = Resource.Success(RegisterConfirmUser())
        var lastRegistrationId: String? = null

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
        ): Resource<RegisterStartUser> = Resource.Success(RegisterStartUser("reg", 1800))

        override suspend fun registerConfirm(registrationId: String): Resource<RegisterConfirmUser> {
            lastRegistrationId = registrationId
            return confirmResult
        }

        override suspend fun login(userName: String, pass: String): Resource<LoginUser> {
            return Resource.Success(LoginUser("", ""))
        }
    }
}
