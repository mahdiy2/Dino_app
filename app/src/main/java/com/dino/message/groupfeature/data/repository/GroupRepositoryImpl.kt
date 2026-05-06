package com.dino.message.groupfeature.data.repository

import android.net.Uri
import com.dino.message.chatfeature.data.remote.dto.MessageAttachment
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.groupfeature.data.db.GroupMessageDao
import com.dino.message.groupfeature.domain.model.AddGroupMembersResult
import com.dino.message.groupfeature.data.remote.GroupApiService
import com.dino.message.groupfeature.domain.model.Group
import com.dino.message.groupfeature.domain.model.GroupHistoryPage
import com.dino.message.groupfeature.domain.model.GroupMessageEntity
import com.dino.message.groupfeature.domain.model.GroupOnlineUser
import com.dino.message.groupfeature.domain.model.MutualFollowersPage
import com.dino.message.groupfeature.domain.repository.GroupRepository
import javax.inject.Inject

class GroupRepositoryImpl @Inject constructor(
    private val apiService: GroupApiService,
    private val groupMessageDao: GroupMessageDao,
) : GroupRepository {

    override suspend fun createGroup(name: String, members: List<String>): Resource<Group> =
        apiService.createGroup(name, members)

    override suspend fun addGroupMembers(
        groupId: Int,
        usernames: List<String>,
    ): Resource<AddGroupMembersResult> = apiService.addGroupMembers(groupId, usernames)

    override suspend fun listGroups(): Resource<List<Group>> =
        apiService.listGroups()

    override suspend fun getGroupDetail(groupId: Int): Resource<Group> =
        apiService.getGroupDetail(groupId)

    override suspend fun getGroupOnlineUsers(groupId: Int): Resource<List<GroupOnlineUser>> =
        apiService.getGroupOnlineUsers(groupId)

    override suspend fun getGroupHistory(
        groupId: Int,
        limit: Int,
        before: String?,
    ): Resource<GroupHistoryPage> = apiService.getGroupHistory(
        groupId = groupId,
        limit = limit,
        before = before,
    )

    override suspend fun getMutualFollowers(page: Int, limit: Int): Resource<MutualFollowersPage> =
        apiService.getMutualFollowers(page, limit)

    override suspend fun uploadGroupAttachment(groupId: Int, fileUri: Uri): Resource<MessageAttachment> =
         apiService.uploadGroupAttachment(groupId, fileUri)

    override suspend fun saveGroupMessage(message: GroupMessageEntity) =
        groupMessageDao.insertMessage(message)

    override suspend fun getGroupMessagesFromDb(groupId: Int, limit: Int, offset: Int): List<GroupMessageEntity> =
        groupMessageDao.getMessagesByGroupId(groupId = groupId, limit = limit, offset = offset)

    override suspend fun getGroupMessageCount(groupId: Int): Int =
        groupMessageDao.getMessageCountByGroupId(groupId)

    override suspend fun upsertGroupMessagesByMessageId(messages: List<GroupMessageEntity>) =
        groupMessageDao.upsertMessagesByMessageId(messages)

    override suspend fun clearGroupMessages(groupId: Int) =
        groupMessageDao.deleteMessagesByGroupId(groupId)

    override suspend fun deleteGroupMessageByMessageId(messageId: String) =
        groupMessageDao.deleteMessageByMessageId(messageId)

    override suspend fun markGroupMessageSeenByMessageId(messageId: String) =
        groupMessageDao.markMessageSeenByMessageId(messageId)

    override suspend fun updateGroupMessageReactionsByMessageId(messageId: String, reactionsJson: String?) =
        groupMessageDao.updateReactionsByMessageId(messageId, reactionsJson)

    override suspend fun reactToMessage(messageId: String, emoji: String?) =
        apiService.reactToMessage(messageId, emoji)
}
