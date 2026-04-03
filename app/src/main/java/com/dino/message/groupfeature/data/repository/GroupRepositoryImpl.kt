package com.dino.message.groupfeature.data.repository

import android.net.Uri
import com.dino.message.chatfeature.data.remote.dto.MessageAttachment
import com.dino.message.corefeature.presentation.util.Resource
 import com.dino.message.groupfeature.data.remote.GroupApiService
 import com.dino.message.groupfeature.domain.model.Group
import com.dino.message.groupfeature.domain.model.GroupMessageEntity
 import com.dino.message.groupfeature.domain.model.MutualFollowersPage
 import com.dino.message.groupfeature.domain.repository.GroupRepository
import com.dino.message.groupfeature.data.db.GroupMessageDao
 import javax.inject.Inject

 class GroupRepositoryImpl @Inject constructor(
     private val apiService: GroupApiService,
    private val groupMessageDao: GroupMessageDao,
 ) : GroupRepository {

    override suspend fun createGroup(name: String, members: List<String>): Resource<Group> =
        apiService.createGroup(name, members)

    override suspend fun listGroups(): Resource<List<Group>> =
        apiService.listGroups()

    override suspend fun getGroupDetail(groupId: Int): Resource<Group> =
        apiService.getGroupDetail(groupId)

    override suspend fun getMutualFollowers(page: Int, limit: Int): Resource<MutualFollowersPage> =
        apiService.getMutualFollowers(page, limit)

    override suspend fun uploadGroupAttachment(groupId: Int, fileUri: Uri): Resource<MessageAttachment> =
         apiService.uploadGroupAttachment(groupId, fileUri)

    override suspend fun saveGroupMessage(message: GroupMessageEntity) =
        groupMessageDao.insertMessage(message)

    override suspend fun getGroupMessagesFromDb(groupId: Int): List<GroupMessageEntity> =
        groupMessageDao.getMessagesByGroupId(groupId)

    override suspend fun clearGroupMessages(groupId: Int) =
        groupMessageDao.deleteMessagesByGroupId(groupId)
 }
