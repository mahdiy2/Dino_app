package com.dino.message.corefeature.domain.settings

import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.domain.model.AppTheme
import kotlinx.coroutines.flow.Flow

interface AppSettingsStore {
    fun getTheme(): Flow<AppTheme>
    suspend fun saveTheme(darkThemeEnabled: String)

    fun getLanguage(): Flow<AppLanguage>
    fun hasLanguageSelection(): Flow<Boolean>
    fun hasCompletedLanguageOnboarding(): Flow<Boolean>
    suspend fun saveLanguage(appLanguage: AppLanguage)
    suspend fun setLanguageOnboardingCompleted(completed: Boolean)

    fun getBadgeAccessEnabled(): Flow<Boolean>
    fun getBadgeAccessLastSyncEpochMillis(): Flow<Long>
    suspend fun saveBadgeAccessCache(hasBadge: Boolean, syncedAtEpochMillis: Long)
    suspend fun clearBadgeAccessCache()

    fun getBadgeFeaturesDialogDismissed(): Flow<Boolean>
    suspend fun setBadgeFeaturesDialogDismissed(dismissed: Boolean)
}
