package com.dino.message.groupfeature.data.remote

import com.dino.message.corefeature.data.remote.BaseRoute

sealed class GroupRoute(url: String) : BaseRoute(url) {
    object CreateGroup : GroupRoute("api/groups")
    object ListGroups : GroupRoute("api/groups")
    object GetUnreadGroups : GroupRoute("api/groups/unread")
    data class GroupDetail(val groupId: Int) : GroupRoute("api/groups/$groupId")
    data class GroupMembers(val groupId: Int) : GroupRoute("api/groups/$groupId/members")
    data class GroupAttachments(val groupId: Int) : GroupRoute("api/groups/$groupId/attachments")
    object MutualFollowers : GroupRoute("api/groups/mutual-followers")
}
