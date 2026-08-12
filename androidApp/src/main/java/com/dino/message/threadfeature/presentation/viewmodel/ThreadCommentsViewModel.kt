package com.dino.message.threadfeature.presentation.viewmodel

import com.dino.message.corefeature.presentation.util.asString
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.data.spref.SPrefManager
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.threadfeature.domain.model.Comment
import com.dino.message.threadfeature.domain.model.VoteTargetType
import com.dino.message.threadfeature.domain.usecase.CreateCommentUseCase
import com.dino.message.threadfeature.domain.usecase.DeleteCommentUseCase
import com.dino.message.threadfeature.domain.usecase.GetCommentsUseCase
import com.dino.message.threadfeature.domain.usecase.VoteUseCase
import com.dino.message.threadfeature.presentation.util.orderCommentsByScoreAndRecency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.dino.message.corefeature.presentation.navigation.AppRoute

class ThreadCommentsViewModel constructor(
    app: Context,
    private val getCommentsUseCase: GetCommentsUseCase,
    private val createCommentUseCase: CreateCommentUseCase,
    private val deleteCommentUseCase: DeleteCommentUseCase,
    private val voteUseCase: VoteUseCase,
    private val sPrefManager: SPrefManager,
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    var postId: Long = 0L
        private set

    private val commentsPageSize = 10
    private var nextCommentsPage = 1

    private val _comments = mutableStateListOf<Comment>()
    val comments: List<Comment> = _comments

    private val _hasMoreComments = MutableStateFlow(true)
    val hasMoreComments: StateFlow<Boolean> = _hasMoreComments.asStateFlow()

    private val _isCommentsLoading = MutableStateFlow(false)
    val isCommentsLoading: StateFlow<Boolean> = _isCommentsLoading.asStateFlow()

    private val _isCommentSending = MutableStateFlow(false)
    val isCommentSending: StateFlow<Boolean> = _isCommentSending.asStateFlow()

    private val _isCommentDeleting = MutableStateFlow(false)
    val isCommentDeleting: StateFlow<Boolean> = _isCommentDeleting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val commentDraft = mutableStateOf("")
    private val replyDraftByComment = mutableStateMapOf<Long, String>()
    private val replyComposerVisibleByComment = mutableStateMapOf<Long, Boolean>()
    private val commentVoteByComment = mutableStateMapOf<Long, Long>()
    private val currentUsername = sPrefManager.getUsername().orEmpty().trim()

    fun closeCommentsBottomSheet() {
        navigateUp(viewModelScope)
    }

    fun openUserProfile(username: String) {
        val normalizedUsername = username.trim()
        if (normalizedUsername.isEmpty()) return

        navigateToDestination(
            AppRoute.Profile(username = normalizedUsername),
            viewModelScope
        )
    }

    fun setPostId(postId: Long) {
        if (this.postId == postId && comments.isNotEmpty()) return
        this.postId = postId
        refreshComments()
    }

    fun refreshComments() {
        if (postId <= 0L) return
        if (_isCommentsLoading.value) return
        _comments.clear()
        nextCommentsPage = 1
        _hasMoreComments.value = true
        loadComments(isRefresh = true)
    }

    fun loadMoreComments() {
        if (postId <= 0L) return
        if (_isCommentsLoading.value) return
        if (!_hasMoreComments.value) return
        loadComments(isRefresh = false)
    }

    private fun loadComments(isRefresh: Boolean) = viewModelScope.launch {
        val targetPage = if (isRefresh) 1 else nextCommentsPage
        getCommentsUseCase.execute(
            postId = postId,
            page = targetPage,
            pageSize = commentsPageSize
        ).collect { res ->
            when (res) {
                is Resource.Success -> {
                    if (isRefresh) _comments.clear()
                    _comments.addAll(res.data)
                    reorderComments()
                    nextCommentsPage = targetPage + 1
                    _hasMoreComments.value = res.data.size >= commentsPageSize
                    _error.value = null
                    _isCommentsLoading.value = false
                }

                is Resource.Loading -> {
                    _isCommentsLoading.value = true
                }

                is Resource.Error -> {
                    _error.value = res.message.asString(getApplication())
                    _isCommentsLoading.value = false
                }
            }
        }
    }

    fun submitComment(parentId: Long? = null) {
        if (_isCommentSending.value) return
        val draft = if (parentId == null) {
            commentDraft.value
        } else {
            replyDraft(parentId)
        }.trim()
        if (draft.isEmpty()) return

        viewModelScope.launch {
            createCommentUseCase.execute(postId = postId, text = draft, parentId = parentId)
                .collect { res ->
                    when (res) {
                        is Resource.Success -> {
                            _isCommentSending.value = false
                            _error.value = null
                            if (parentId == null) {
                                commentDraft.value = ""
                            } else {
                                replyDraftByComment[parentId] = ""
                                replyComposerVisibleByComment[parentId] = false
                            }
                            refreshComments()
                        }

                        is Resource.Loading -> {
                            _isCommentSending.value = true
                        }

                        is Resource.Error -> {
                            _isCommentSending.value = false
                            _error.value = res.message.asString(getApplication())
                        }
                    }
                }
        }
    }

    fun isReplyComposerVisible(commentId: Long): Boolean = replyComposerVisibleByComment[commentId] == true

    fun toggleReplyComposer(commentId: Long) {
        replyComposerVisibleByComment[commentId] = !isReplyComposerVisible(commentId)
    }

    fun replyDraft(commentId: Long): String = replyDraftByComment[commentId] ?: ""

    fun onReplyDraftChanged(commentId: Long, value: String) {
        replyDraftByComment[commentId] = value
    }

    fun selectedCommentVote(commentId: Long): Long = commentVoteByComment[commentId] ?: 0L

    fun canDeleteComment(comment: Comment): Boolean {
        return comment.author.username.equals(currentUsername, ignoreCase = true)
    }

    fun deleteOwnComment(commentId: Long) {
        if (_isCommentDeleting.value) return

        val targetComment = findCommentById(_comments, commentId) ?: return
        if (!canDeleteComment(targetComment)) return

        viewModelScope.launch {
            deleteCommentUseCase.execute(commentId).collect { res ->
                when (res) {
                    is Resource.Success -> {
                        _isCommentDeleting.value = false
                        _error.value = null
                        refreshComments()
                    }
                    is Resource.Loading -> {
                        _isCommentDeleting.value = true
                    }
                    is Resource.Error -> {
                        _isCommentDeleting.value = false
                        _error.value = res.message.asString(getApplication())
                    }
                }
            }
        }
    }

    fun voteComment(commentId: Long, value: Long) {
        val oldVote = selectedCommentVote(commentId)
        val delta = when {
            oldVote.toLong() == value -> 0
            oldVote == 0L -> value
            oldVote == 1L && value == (-1L) -> -2
            oldVote == -1L && value == 1L -> 2
            else -> 0
        }

        if (delta != 0L) {
            updateCommentScore(commentId = commentId, delta = delta)
        }
        commentVoteByComment[commentId] = value

        viewModelScope.launch {
            voteUseCase.execute(
                targetType = VoteTargetType.COMMENT,
                targetId = commentId,
                value = value
            ).collect { res ->
                when (res) {
                    is Resource.Success -> _error.value = null
                    is Resource.Loading -> Unit
                    is Resource.Error -> {
                        commentVoteByComment[commentId] = oldVote
                        if (delta != 0L) {
                            updateCommentScore(commentId = commentId, delta = -delta)
                        }
                        _error.value = res.message.asString(getApplication())
                    }
                }
            }
        }
    }

    private fun updateCommentScore(commentId: Long, delta: Long) {
        val updated = updateCommentScoreRecursive(
            comments = _comments,
            commentId = commentId,
            delta = delta
        )
        _comments.clear()
        _comments.addAll(orderCommentsByScoreAndRecency(updated))
    }

    private fun updateCommentScoreRecursive(
        comments: List<Comment>,
        commentId: Long,
        delta: Long
    ): List<Comment> {
        return comments.map { comment ->
            when {
                comment.id == commentId -> comment.copy(score = comment.score + delta)
                comment.replies.isNotEmpty() -> comment.copy(
                    replies = updateCommentScoreRecursive(
                        comments = comment.replies,
                        commentId = commentId,
                        delta = delta
                    )
                )

                else -> comment
            }
        }
    }

    private fun findCommentById(comments: List<Comment>, commentId: Long): Comment? {
        comments.forEach { comment ->
            if (comment.id == commentId) return comment
            val fromChild = findCommentById(comment.replies, commentId)
            if (fromChild != null) return fromChild
        }
        return null
    }

    private fun reorderComments() {
        val ordered = orderCommentsByScoreAndRecency(_comments.toList())
        _comments.clear()
        _comments.addAll(ordered)
    }
}
