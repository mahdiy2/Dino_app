package com.dino.message.groupfeature.data.remote

import com.dino.message.corefeature.data.remote.BaseRoute

sealed class GroupRoute(url: String) : BaseRoute(url) {
    object CreateGroup : GroupRoute("api/groups")
    object ListGroups : GroupRoute("api/groups")
    object PublicRoomStatus : GroupRoute("api/groups/public-room")
    object JoinPublicRoom : GroupRoute("api/groups/public-room/join")
    object GetUnreadGroups : GroupRoute("api/groups/unread")
    data class GroupDetail(val groupId: Int) : GroupRoute("api/groups/$groupId")
    data class LeaveGroup(val groupId: Int) : GroupRoute("api/groups/$groupId/leave")
    data class GroupMembers(val groupId: Int) : GroupRoute("api/groups/$groupId/members")
    data class GroupOnlineUsers(val groupId: Int) : GroupRoute("api/groups/$groupId/online-users")
    data class GroupAttachments(val groupId: Int) : GroupRoute("api/groups/$groupId/attachments")
    data class GroupHistory(val groupId: Int) : GroupRoute("api/messages/history/group/$groupId")
    object MutualFollowers : GroupRoute("api/groups/mutual-followers")
}
