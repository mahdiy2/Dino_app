package com.dino.message.authfeature.domain.usecase

import com.dino.message.authfeature.domain.model.LoginUser
import com.dino.message.authfeature.domain.model.KeyStatusUser
import com.dino.message.authfeature.domain.model.RegisterConfirmUser
import com.dino.message.authfeature.domain.model.RegisterStartUser
import com.dino.message.authfeature.domain.model.RegisterUser
import com.dino.message.authfeature.domain.model.RotatePublicKeyUser
import com.dino.message.authfeature.domain.repository.AuthRepository
import com.dino.message.corefeature.domain.model.asStringResourceContent
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.profilefeature.domain.model.ProfileFollowResult
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class ConfirmRegistrationUseCaseTest {

    @Test
    fun `execute emits loading then success`() = runTest {
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
    fun `execute emits loading then error`() = runTest {
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
            publicKey: String,
            referralCode: String?,
            deviceId: String?
        ): Resource<RegisterUser> = Resource.Success(RegisterUser())

        override suspend fun registerStart(
            userName: String,
            pass: String,
            publicKey: String,
            clientNonce: String,
            referralCode: String?,
            deviceId: String?
        ): Resource<RegisterStartUser> = Resource.Success(RegisterStartUser("reg", 1800))

        override suspend fun registerConfirm(registrationId: String): Resource<RegisterConfirmUser> {
            lastRegistrationId = registrationId
            return confirmResult
        }

        override suspend fun login(userName: String, pass: String): Resource<LoginUser> {
            return Resource.Success(LoginUser("", ""))
        }

        override suspend fun getKeyStatus(): Resource<KeyStatusUser> {
            return Resource.Success(KeyStatusUser(hasServerPublicKey = true))
        }

        override suspend fun rotatePublicKey(publicKey: String): Resource<RotatePublicKeyUser> {
            return Resource.Success(RotatePublicKeyUser())
        }

        override suspend fun updateDeviceId(deviceId: String): Resource<ProfileFollowResult> {
            return Resource.Success(ProfileFollowResult("ok"))
        }
    }
}
