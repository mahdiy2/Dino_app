package com.dino.message.mainpage.domain.usecase

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.mainpage.domain.model.DetailedContact
import com.dino.message.mainpage.domain.repository.MainRepository
import kotlinx.coroutines.flow.flow

class GetContactsDeltaUseCase constructor(
    private val mainRepository: MainRepository,
) {
    fun execute(usernames: List<String>) = flow<Resource<List<DetailedContact>>> {
        emit(Resource.Loading())
        emit(mainRepository.getContactsDelta(usernames))
    }
}
