package com.dino.message.authfeature.domain.usecase

import com.dino.message.corefeature.domain.device.DeviceInfoProvider
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.corefeature.domain.session.LocalSessionStore
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class SyncDeviceIdUseCase constructor(
    private val localSessionStore: LocalSessionStore,
    private val deviceIdProvider: DeviceInfoProvider,
    private val updateDeviceIdUseCase: UpdateDeviceIdUseCase,
) {
    private val isSendingDeviceId = AtomicBoolean(false)

    fun execute() = flow<Resource<Unit>> {
        if (!isSendingDeviceId.compareAndSet(false, true)) {
            emit(Resource.Success(Unit))
            return@flow
        }

        try {
            val username = localSessionStore.getUsername()?.trim().orEmpty()
            if (username.isBlank() || !localSessionStore.getUserLogIn()) {
                emit(Resource.Success(Unit))
                return@flow
            }

            val currentDeviceId = deviceIdProvider.getDeviceId()
            val syncedUser = localSessionStore.getDeviceSyncUserId()?.trim().orEmpty()
            val syncedDeviceId = localSessionStore.getDeviceSyncDeviceId()?.trim().orEmpty()

            if (syncedUser == username && syncedDeviceId == currentDeviceId) {
                emit(Resource.Success(Unit))
                return@flow
            }

            val result = updateDeviceIdUseCase.execute(currentDeviceId)
                .first { it !is Resource.Loading }

            when (result) {
                is Resource.Success -> {
                    localSessionStore.saveDeviceSyncUserId(username)
                    localSessionStore.saveDeviceSyncDeviceId(currentDeviceId)
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
