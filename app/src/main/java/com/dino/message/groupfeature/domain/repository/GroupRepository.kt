package com.dino.message.groupfeature.domain.repository

import android.net.Uri
import com.dino.message.chatfeature.data.remote.dto.MessageAttachment
import com.dino.message.corefeature.presentation.util.Resource
 import com.dino.message.groupfeature.domain.model.Group
import com.dino.message.groupfeature.domain.model.GroupMessageEntity
 import com.dino.message.groupfeature.domain.model.MutualFollowersPage

 interface GroupRepository {
     suspend fun createGroup(name: String, members: List<String>): Resource<Group>
     suspend fun listGroups(): Resource<List<Group>>
     suspend fun getGroupDetail(groupId: Int): Resource<Group>
     suspend fun getMutualFollowers(page: Int = 1, limit: Int = 50): Resource<MutualFollowersPage>
     suspend fun uploadGroupAttachment(groupId: Int, fileUri: Uri): Resource<MessageAttachment>
    suspend fun saveGroupMessage(message: GroupMessageEntity)
    suspend fun getGroupMessagesFromDb(groupId: Int): List<GroupMessageEntity>
    suspend fun clearGroupMessages(groupId: Int)
 }
