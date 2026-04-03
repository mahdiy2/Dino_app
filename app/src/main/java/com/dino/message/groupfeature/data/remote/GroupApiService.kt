package com.dino.message.groupfeature.data.remote

import android.net.Uri
import com.dino.message.chatfeature.data.remote.dto.MessageAttachment
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.groupfeature.domain.model.Group
import com.dino.message.groupfeature.domain.model.MutualFollowersPage

interface GroupApiService {
    suspend fun createGroup(name: String, members: List<String>): Resource<Group>
    suspend fun listGroups(): Resource<List<Group>>
    suspend fun getGroupDetail(groupId: Int): Resource<Group>
    suspend fun getMutualFollowers(page: Int, limit: Int): Resource<MutualFollowersPage>
    suspend fun uploadGroupAttachment(groupId: Int, fileUri: Uri): Resource<MessageAttachment>
}
