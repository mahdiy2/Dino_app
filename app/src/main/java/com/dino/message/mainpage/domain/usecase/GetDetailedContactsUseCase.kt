package com.dino.message.mainpage.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.mainpage.domain.repository.MainRepository
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetDetailedContactsUseCase @Inject constructor(
    private val mainRepository: MainRepository
) {

    fun execute() = flow {
        emit(Resource.Loading())
        emit(mainRepository.getDetailedContacts())
    }
}
