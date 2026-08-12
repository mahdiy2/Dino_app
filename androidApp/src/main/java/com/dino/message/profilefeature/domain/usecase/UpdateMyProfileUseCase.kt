package com.dino.message.profilefeature.domain.usecase

import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.profilefeature.domain.model.Profile
import com.dino.message.profilefeature.domain.model.ProfileUpdateImage
import com.dino.message.profilefeature.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.flow

class UpdateMyProfileUseCase constructor(
    private val profileRepository: ProfileRepository
) {
    fun execute(
        name: String? = null,
        bio: String? = null,
        profileImageShape: String? = null,
        profileImage: ProfileUpdateImage? = null,
        profileVideo: ProfileUpdateImage? = null,
        removeProfileImage: Boolean = false,
        removeProfileVideo: Boolean = false,
    ) = flow<Resource<Profile>> {
        emit(Resource.Loading())
        emit(
            profileRepository.updateMyProfile(
                name = name,
                bio = bio,
                profileImageShape = profileImageShape,
                profileImage = profileImage,
                profileVideo = profileVideo,
                removeProfileImage = removeProfileImage,
                removeProfileVideo = removeProfileVideo,
            )
        )
    }
}
