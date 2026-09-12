package com.dino.message.profilefeature.presentation.viewmodel

import com.dino.message.corefeature.presentation.util.asString
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.corefeature.domain.resource.StringResolver
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.corefeature.presentation.util.UiEventBus
import com.dino.message.corefeature.presentation.viewmodel.ScopedStateHolder
import com.dino.message.threadfeature.domain.usecase.DeletePostUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeletePostBottomSheetViewModel constructor(
    private val stringResolver: StringResolver,
    uiEventBus: UiEventBus,
    private val deletePostUseCase: DeletePostUseCase,
) : ScopedStateHolder(),
    HaveUIEvent by HaveUIEventImpl(uiEventBus) {


    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun confirmDelete(postId: Long) {
        if (_isDeleting.value || postId <= 0L) return

        viewModelScope.launch {
            deletePostUseCase.execute(postId).collect { res ->
                when (res) {
                    is Resource.Loading -> {
                        _isDeleting.value = true
                    }
                    is Resource.Success -> {
                        _isDeleting.value = false
                        _error.value = null
                        navigateUp(viewModelScope)
                    }
                    is Resource.Error -> {
                        _isDeleting.value = false
                        _error.value = res.message.resolveWith(stringResolver)
                    }
                }
            }
        }
    }

    fun closeSheet() {
        if (_isDeleting.value) return
        navigateUp(viewModelScope)
    }
}
