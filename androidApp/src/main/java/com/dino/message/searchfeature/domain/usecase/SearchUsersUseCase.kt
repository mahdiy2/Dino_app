package com.dino.message.searchfeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.searchfeature.domain.repository.SearchRepository
import kotlinx.coroutines.flow.flow

class SearchUsersUseCase constructor(
    private val searchRepository: SearchRepository
) {
    fun execute(query: String, page: Int, limit: Int) = flow {
        emit(Resource.Loading())
        emit(searchRepository.searchUsers(query = query, page = page, limit = limit))
    }
}
