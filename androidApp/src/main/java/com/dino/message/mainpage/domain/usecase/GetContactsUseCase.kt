package com.dino.message.mainpage.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.mainpage.domain.repository.MainRepository
import kotlinx.coroutines.flow.flow

class GetContactsUseCase constructor(
    private val mainRepository: MainRepository
) {

    fun execute(username: String) = flow {
        emit(Resource.Loading())
        emit(
            mainRepository.getContacts(
                username
            )
        )
    }
}