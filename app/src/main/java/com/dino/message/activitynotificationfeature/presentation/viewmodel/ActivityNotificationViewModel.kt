package com.dino.message.activitynotificationfeature.presentation.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.activitynotificationfeature.domain.model.ActivityNotification
import com.dino.message.activitynotificationfeature.domain.usecase.GetActivityNotificationsUseCase
import com.dino.message.activitynotificationfeature.domain.usecase.MarkNotificationsReadUseCase
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.profilefeature.domain.usecase.FollowUserUseCase
import com.dino.message.profilefeature.domain.usecase.GetFollowStatusUseCase
import com.dino.message.threadfeature.domain.usecase.CreateCommentUseCase
import com.ramcosta.composedestinations.generated.destinations.ProfileScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SharedPostDetailsScreenDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ActivityNotificationViewModel @Inject constructor(
    @ApplicationContext app: Context,
    private val getActivityNotificationsUseCase: GetActivityNotificationsUseCase,
    private val markNotificationsReadUseCase: MarkNotificationsReadUseCase,
    private val getFollowStatusUseCase: GetFollowStatusUseCase,
    private val followUserUseCase: FollowUserUseCase,
    private val createCommentUseCase: CreateCommentUseCase
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    private val pageLimit = 20
    private var currentPage = 1
    private var totalNotifications = Int.MAX_VALUE
    private var isRequestRunning = false

    private val _notifications = mutableStateListOf<ActivityNotification>()
    val notifications: List<ActivityNotification> = _notifications
    private val followStatusByUsername = mutableStateMapOf<String, Boolean>()
    private val followStatusLoadingByUsername = mutableStateMapOf<String, Boolean>()
    private val followActionLoadingByUsername = mutableStateMapOf<String, Boolean>()
    private val replyComposerVisibleByNotification = mutableStateMapOf<Long, Boolean>()
    private val replyDraftByNotification = mutableStateMapOf<Long, String>()
    private val replySendingByNotification = mutableStateMapOf<Long, Boolean>()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        if (isRequestRunning) return
        currentPage = 1
        totalNotifications = Int.MAX_VALUE
        _notifications.clear()
        followStatusByUsername.clear()
        followStatusLoadingByUsername.clear()
        followActionLoadingByUsername.clear()
        replyComposerVisibleByNotification.clear()
        replyDraftByNotification.clear()
        replySendingByNotification.clear()
        requestPage(isRefresh = true)
    }

    fun loadNextPage() {
        if (isRequestRunning) return
        if (_notifications.size >= totalNotifications) return
        requestPage(isRefresh = false)
    }

    private fun requestPage(isRefresh: Boolean) = viewModelScope.launch {
        if (isRequestRunning) return@launch
        isRequestRunning = true

        getActivityNotificationsUseCase.execute(
            page = currentPage,
            limit = pageLimit
        ).collect { res ->
            when (res) {
                is Resource.Success -> {
                    if (isRefresh) _notifications.clear()
                    totalNotifications = res.data.total
                    _notifications.addAll(res.data.notifications)
                    prefetchFollowStatuses(res.data.notifications)
                    if (res.data.notifications.isNotEmpty()) {
                        currentPage = res.data.page + 1
                    }
                    _error.value = null
                    showLoading(isRefresh = false, isLoadMore = false)
                    isRequestRunning = false

                    markAllRead()
                }

                is Resource.Loading -> {
                    showLoading(isRefresh = isRefresh, isLoadMore = !isRefresh)
                }

                is Resource.Error -> {
                    _error.value = res.message.asString(getApplication())
                    showLoading(isRefresh = false, isLoadMore = false)
                    isRequestRunning = false
                }
            }
        }
    }

    private fun markAllRead() = viewModelScope.launch {
        markNotificationsReadUseCase.execute().collect()
    }

    private fun prefetchFollowStatuses(notifications: List<ActivityNotification>) {
        notifications
            .asSequence()
            .filter { it.kind == "follow" }
            .map { it.actor.username }
            .distinct()
            .forEach(::ensureFollowStatus)
    }

    private fun ensureFollowStatus(username: String) {
        if (followStatusByUsername.containsKey(username)) return
        if (followStatusLoadingByUsername[username] == true) return

        viewModelScope.launch {
            getFollowStatusUseCase.execute(username).collect { res ->
                when (res) {
                    is Resource.Success -> {
                        followStatusByUsername[username] = res.data
                        followStatusLoadingByUsername[username] = false
                    }

                    is Resource.Loading -> {
                        followStatusLoadingByUsername[username] = true
                    }

                    is Resource.Error -> {
                        followStatusLoadingByUsername[username] = false
                    }
                }
            }
        }
    }

    fun shouldShowFollowBack(notification: ActivityNotification): Boolean {
        if (notification.kind != "follow") return false
        val username = notification.actor.username
        val followStatus = followStatusByUsername[username]
        return followStatus == false
    }

    fun isFollowBackLoading(username: String): Boolean =
        followActionLoadingByUsername[username] == true

    fun followBack(username: String) {
        if (followActionLoadingByUsername[username] == true) return

        viewModelScope.launch {
            followUserUseCase.execute(username).collect { res ->
                when (res) {
                    is Resource.Success -> {
                        followStatusByUsername[username] = true
                        followActionLoadingByUsername[username] = false
                    }

                    is Resource.Loading -> {
                        followActionLoadingByUsername[username] = true
                    }

                    is Resource.Error -> {
                        _error.value = res.message.asString(getApplication())
                        followActionLoadingByUsername[username] = false
                    }
                }
            }
        }
    }

    fun openUserProfile(username: String) {
        navigateToDestination(ProfileScreenDestination(username = username), viewModelScope)
    }

    fun shouldOpenPostDetails(notification: ActivityNotification): Boolean {
        return resolvePostId(notification) != null && (
            notification.kind == "comment" ||
                notification.kind == "comment_reply" ||
                isUpvoteNotification(notification)
            )
    }

    fun openNotification(notification: ActivityNotification) {
        val postId = resolvePostId(notification)
        if (postId != null && shouldOpenPostDetails(notification)) {
            navigateToDestination(
                SharedPostDetailsScreenDestination(postId = postId),
                viewModelScope
            )
            return
        }

        openUserProfile(notification.actor.username)
    }

    fun canReply(notification: ActivityNotification): Boolean {
        if (notification.kind != "comment" && notification.kind != "comment_reply") return false
        return resolvePostId(notification) != null && resolveParentCommentId(notification) != null
    }

    fun isReplyComposerVisible(notificationId: Long): Boolean =
        replyComposerVisibleByNotification[notificationId] == true

    fun toggleReplyComposer(notificationId: Long) {
        replyComposerVisibleByNotification[notificationId] = !isReplyComposerVisible(notificationId)
    }

    fun replyDraft(notificationId: Long): String = replyDraftByNotification[notificationId] ?: ""

    fun onReplyDraftChanged(notificationId: Long, value: String) {
        replyDraftByNotification[notificationId] = value
    }

    fun isReplySending(notificationId: Long): Boolean = replySendingByNotification[notificationId] == true

    fun submitReply(notification: ActivityNotification) {
        if (!canReply(notification)) return
        if (isReplySending(notification.id)) return

        val draft = replyDraft(notification.id).trim()
        if (draft.isEmpty()) return

        val postId = resolvePostId(notification) ?: return
        val parentCommentId = resolveParentCommentId(notification) ?: return

        viewModelScope.launch {
            createCommentUseCase.execute(
                postId = postId,
                text = draft,
                parentId = parentCommentId
            ).collect { res ->
                when (res) {
                    is Resource.Success -> {
                        replySendingByNotification[notification.id] = false
                        replyDraftByNotification[notification.id] = ""
                        replyComposerVisibleByNotification[notification.id] = false
                        _error.value = null
                    }

                    is Resource.Loading -> {
                        replySendingByNotification[notification.id] = true
                    }

                    is Resource.Error -> {
                        replySendingByNotification[notification.id] = false
                        _error.value = res.message.asString(getApplication())
                    }
                }
            }
        }
    }

    private fun resolvePostId(notification: ActivityNotification): Long? {
        return notification.extra?.post_id
            ?: if (notification.target_type == "post") notification.target_id else null
    }

    private fun resolveParentCommentId(notification: ActivityNotification): Long? {
        return notification.extra?.comment_id
            ?: if (notification.target_type == "comment") notification.target_id else null
    }

    private fun isUpvoteNotification(notification: ActivityNotification): Boolean {
        if (notification.kind != "vote") return false
        val voteValue = notification.extra?.vote_value
        val voteLabel = notification.extra?.vote_label?.lowercase()
        return voteValue == 1 || voteLabel == "upvote"
    }

    private fun showLoading(isRefresh: Boolean, isLoadMore: Boolean) {
        _isRefreshing.value = isRefresh
        _isLoadingMore.value = isLoadMore
    }

    fun navigateBack() {
        navigateUp(viewModelScope)
    }
}
