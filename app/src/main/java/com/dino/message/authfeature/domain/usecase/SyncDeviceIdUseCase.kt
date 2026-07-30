package com.dino.message.authfeature.domain.usecase

import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.data.util.DeviceIdProvider
import com.dino.message.corefeature.presentation.util.Resource
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class SyncDeviceIdUseCase constructor(
    private val sPrefManager: SPrefManager,
    private val deviceIdProvider: DeviceIdProvider,
    private val updateDeviceIdUseCase: UpdateDeviceIdUseCase,
) {
    private val isSendingDeviceId = AtomicBoolean(false)

    fun execute() = flow<Resource<Unit>> {
        if (!isSendingDeviceId.compareAndSet(false, true)) {
            emit(Resource.Success(Unit))
            return@flow
        }

        try {
            val username = sPrefManager.getUsername()?.trim().orEmpty()
            if (username.isBlank() || !sPrefManager.getUserLogIn()) {
                emit(Resource.Success(Unit))
                return@flow
            }

            val currentDeviceId = deviceIdProvider.getDeviceId()
            val syncedUser = sPrefManager.getDeviceSyncUserId()?.trim().orEmpty()
            val syncedDeviceId = sPrefManager.getDeviceSyncDeviceId()?.trim().orEmpty()

            if (syncedUser == username && syncedDeviceId == currentDeviceId) {
                emit(Resource.Success(Unit))
                return@flow
            }

            val result = updateDeviceIdUseCase.execute(currentDeviceId)
                .first { it !is Resource.Loading }

            when (result) {
                is Resource.Success -> {
                    sPrefManager.saveDeviceSyncUserId(username)
                    sPrefManager.saveDeviceSyncDeviceId(currentDeviceId)
                    emit(Resource.Success(Unit))
                }

                is Resource.Error -> {
                    emit(Resource.Error(result.message))
                }

                is Resource.Loading -> Unit
            }
        } finally {
            isSendingDeviceId.set(false)
        }
    }
}
