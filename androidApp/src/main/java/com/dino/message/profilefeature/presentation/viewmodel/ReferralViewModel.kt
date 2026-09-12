package com.dino.message.profilefeature.presentation.viewmodel

import com.dino.message.corefeature.domain.settings.AppSettingsStore
import com.dino.message.corefeature.domain.result.Resource
import com.dino.message.corefeature.domain.resource.StringResolver
import com.dino.message.corefeature.domain.system.AppClock
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEvent
import com.dino.message.corefeature.presentation.viewmodel.HaveUIEventImpl
import com.dino.message.corefeature.presentation.util.UiEventBus
import com.dino.message.corefeature.presentation.viewmodel.ScopedStateHolder
import com.dino.message.profilefeature.domain.share.ReferralShareLauncher
import com.dino.message.profilefeature.domain.model.ReferralSummary
import com.dino.message.profilefeature.domain.usecase.GetMyReferralSummaryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReferralViewModel constructor(
    private val stringResolver: StringResolver,
    uiEventBus: UiEventBus,
    private val dataStoreUtil: AppSettingsStore,
    private val getMyReferralSummaryUseCase: GetMyReferralSummaryUseCase,
    private val referralShareLauncher: ReferralShareLauncher,
    private val appClock: AppClock,
) : ScopedStateHolder(),
    HaveUIEvent by HaveUIEventImpl(uiEventBus) {

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
                    dataStoreUtil.saveBadgeAccessCache(
                        hasBadge = hasVerifiedReferralBadge(res.data),
                        syncedAtEpochMillis = appClock.currentTimeMillis(),
                    )
                }

                is Resource.Error -> {
                    _isLoading.value = false
                    _error.value = res.message.resolveWith(stringResolver)
                }
            }
        }
    }

    fun shareReferralInvite() {
        val referralSummary = _summary.value ?: return
        val shareText = buildPersianShareText(referralSummary)
        referralShareLauncher.shareInvite(shareText)
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

    private fun hasVerifiedReferralBadge(summary: ReferralSummary): Boolean {
        if (summary.isVerified) return true
        val required = summary.requiredInvitesForVerified
        return required > 0 && summary.joinedCount >= required
    }

    fun navigateUp() {
        navigateUp(viewModelScope)
    }
}
