package com.dino.message.profilefeature.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.threadfeature.domain.usecase.DeletePostUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeletePostBottomSheetViewModel constructor(
    app: Context,
    private val deletePostUseCase: DeletePostUseCase,
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {


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
                        _error.value = res.message.asString(getApplication())
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
