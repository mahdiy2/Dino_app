package com.dino.message.mainpage.domain.usecase

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.mainpage.domain.repository.MainRepository
import kotlinx.coroutines.flow.flow

class AddContactUseCase constructor(
    private val mainRepository: MainRepository
){
    fun execute(contact: String) = flow {
        emit(Resource.Loading())
        emit(
            mainRepository.addContact(
                contact
            )
        )
    }
}