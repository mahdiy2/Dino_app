package com.dino.message.profilefeature.domain.usecase

import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.profilefeature.domain.repository.ProfileRepository
import com.dino.message.threadfeature.domain.model.PostsPage
import kotlinx.coroutines.flow.flow

class GetProfilePostsUseCase constructor(
    private val profileRepository: ProfileRepository
) {
    fun execute(username: String, page: Int, limit: Int) = flow<Resource<PostsPage>> {
        emit(Resource.Loading())
        emit(profileRepository.getUserPosts(username = username, page = page, limit = limit))
    }
}
