package com.dino.message.profilefeature.presentation.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dino.message.DinoOrderApplication
import com.dino.message.R
import com.dino.message.corefeature.presentation.util.Resource
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.profilefeature.domain.model.ReferralSummary
import com.dino.message.profilefeature.domain.usecase.GetMyReferralSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReferralViewModel @Inject constructor(
    @ApplicationContext app: Context,
    private val getMyReferralSummaryUseCase: GetMyReferralSummaryUseCase,
) : AndroidViewModel(app as DinoOrderApplication),
    HaveUIEvent by HaveUIEventImpl(app as DinoOrderApplication) {

    private val _summary = MutableStateFlow<ReferralSummary?>(null)
    val summary: StateFlow<ReferralSummary?> = _summary.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun load() = viewModelScope.launch {
        getMyReferralSummaryUseCase.execute().collect { res ->
            when (res) {
                is Resource.Loading -> {
                    _isLoading.value = true
                }

                is Resource.Success -> {
                    _isLoading.value = false
                    _summary.value = res.data
                    _error.value = null
                }

                is Resource.Error -> {
                    _isLoading.value = false
                    _error.value = res.message.asString(getApplication())
                }
            }
        }
    }

    fun shareReferralInvite() {
        val referralSummary = _summary.value ?: return
        val context = getApplication<DinoOrderApplication>()
        val shareText = buildPersianShareText(referralSummary)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        val chooser = Intent.createChooser(
            sendIntent,
            context.getString(R.string.referral_share_sheet_title)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    private fun buildPersianShareText(summary: ReferralSummary): String {
        return """
سلام 👋
من دارم از اپ Dino استفاده می‌کنم، خیلی جذابه! تو هم نصبش کن و بیا داخل برنامه:
${summary.downloadLink}

کد دعوت من:
${summary.referralCode}
        """.trimIndent()
    }

    fun navigateUp() {
        navigateUp(viewModelScope)
    }
}
