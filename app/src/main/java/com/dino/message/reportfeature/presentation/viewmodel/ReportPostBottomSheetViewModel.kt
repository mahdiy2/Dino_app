package com.dino.message.reportfeature.presentation.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.R
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.reportfeature.domain.usecase.CreatePostReportUseCase
import com.dino.message.reportfeature.domain.usecase.GetReportTypesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportPostBottomSheetViewModel @Inject constructor(
    @ApplicationContext app: Context,
    private val getReportTypesUseCase: GetReportTypesUseCase,
    private val createPostReportUseCase: CreatePostReportUseCase,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    val postId: Long = savedStateHandle.get<Long>("postId") ?: 0L

    private val _reportTypes = mutableStateListOf<String>()
    val reportTypes: List<String> = _reportTypes

    val selectedType = mutableStateOf<String?>(null)
    val description = mutableStateOf("")

    private val _isTypesLoading = MutableStateFlow(false)
    val isTypesLoading: StateFlow<Boolean> = _isTypesLoading.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadReportTypes()
    }

    fun loadReportTypes() = viewModelScope.launch {
        getReportTypesUseCase.execute().collect { res ->
            when (res) {
                is Resource.Loading -> {
                    _isTypesLoading.value = true
                }

                is Resource.Success -> {
                    _isTypesLoading.value = false
                    _error.value = null
                    _reportTypes.clear()
                    _reportTypes.addAll(res.data)
                    if (selectedType.value !in res.data) {
                        selectedType.value = null
                    }
                }

                is Resource.Error -> {
                    _isTypesLoading.value = false
                    _error.value = res.message.asString(getApplication())
                }
            }
        }
    }

    fun onSelectType(type: String) {
        selectedType.value = type
        if (_error.value != null) {
            _error.value = null
        }
    }

    fun onDescriptionChange(value: String) {
        description.value = value.take(255)
    }

    fun submitReport() {
        if (_isSubmitting.value) return

        val type = selectedType.value
        if (type.isNullOrBlank()) {
            _error.value =
                getApplication<DinoOrderApplication>().getString(R.string.report_post_select_type_error)
            return
        }

        val normalizedDescription = description.value.trim().takeIf { it.isNotEmpty() }

        viewModelScope.launch {
            createPostReportUseCase.execute(
                postId = postId,
                reportType = type,
                description = normalizedDescription,
            ).collect { res ->
                when (res) {
                    is Resource.Loading -> {
                        _isSubmitting.value = true
                    }

                    is Resource.Success -> {
                        _isSubmitting.value = false
                        _error.value = null
                        navigateUp(viewModelScope)
                    }

                    is Resource.Error -> {
                        _isSubmitting.value = false
                        _error.value = res.message.asString(getApplication())
                    }
                }
            }
        }
    }

    fun closeSheet() {
        navigateUp(viewModelScope)
    }
}
