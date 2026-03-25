package com.dino.message.activitynotificationfeature.presentation.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.activitynotificationfeature.domain.model.ActivityNotification
import com.dino.message.activitynotificationfeature.domain.usecase.GetActivityNotificationsUseCase
import com.dino.message.activitynotificationfeature.domain.usecase.MarkNotificationsReadUseCase
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
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
    private val markNotificationsReadUseCase: MarkNotificationsReadUseCase
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    private val pageLimit = 20
    private var currentPage = 1
    private var totalNotifications = Int.MAX_VALUE
    private var isRequestRunning = false

    private val _notifications = mutableStateListOf<ActivityNotification>()
    val notifications: List<ActivityNotification> = _notifications

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

    private fun showLoading(isRefresh: Boolean, isLoadMore: Boolean) {
        _isRefreshing.value = isRefresh
        _isLoadingMore.value = isLoadMore
    }

    fun navigateBack() {
        navigateUp(viewModelScope)
    }
}
