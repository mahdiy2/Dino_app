package com.dino.message.reportfeature.presentation.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.dino.message.corefeature.domain.model.AppStringKey
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.corefeature.domain.resource.StringResolver
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.corefeature.presentation.util.UiEventBus
import com.dino.message.corefeature.presentation.viewmodel.ScopedStateHolder
import com.dino.message.reportfeature.domain.usecase.CreatePostReportUseCase
import com.dino.message.reportfeature.domain.usecase.GetReportTypesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReportPostBottomSheetViewModel constructor(
    private val stringResolver: StringResolver,
    uiEventBus: UiEventBus,
    private val getReportTypesUseCase: GetReportTypesUseCase,
    private val createPostReportUseCase: CreatePostReportUseCase,
) : ScopedStateHolder(),
    HaveUIEvent by HaveUIEventImpl(uiEventBus) {


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
                    _error.value = res.message.resolveWith(stringResolver)
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

    fun submitReport(postId: Long) {
        if (_isSubmitting.value) return

        val type = selectedType.value
        if (type.isNullOrBlank()) {
            _error.value =
                stringResolver.resolve(AppStringKey.ReportPostSelectTypeError)
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
                        _error.value = res.message.resolveWith(stringResolver)
                    }
                }
            }
        }
    }

    fun closeSheet() {
        navigateUp(viewModelScope)
    }
}
