package com.dino.message.chatfeature.domain.usecase

import com.dino.message.chatfeature.domain.repository.ChatRepository
import com.dino.message.corefeature.domain.result.Resource
import kotlinx.coroutines.flow.flow

class GetReceiverPublicKeyUseCase constructor(
    private val chatRepository: ChatRepository,
){
    fun execute(username: String) = flow {
        emit(Resource.Loading())
        emit(chatRepository.getReceiverPublicKey(username))
    }
}