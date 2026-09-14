package com.dino.message.corefeature.data.spref

import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.domain.model.AppTheme
import com.dino.message.corefeature.domain.settings.AppSettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import platform.Foundation.NSUserDefaults

class IosAppSettingsStore(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : AppSettingsStore {
    private val theme = MutableStateFlow(readTheme())
    private val language = MutableStateFlow(readLanguage())
    private val hasLanguage = MutableStateFlow(defaults.objectForKey(AppSettingsKeys.LANGUAGE) != null)
    private val onboardingDone = MutableStateFlow(defaults.boolForKey(AppSettingsKeys.LANGUAGE_ONBOARDING_DONE))
    private val badgeEnabled = MutableStateFlow(defaults.boolForKey(AppSettingsKeys.BADGE_ACCESS_ENABLED))
    private val badgeLastSync = MutableStateFlow(defaults.objectForKey(AppSettingsKeys.BADGE_ACCESS_LAST_SYNC_EPOCH_MS)?.let {
        defaults.integerForKey(AppSettingsKeys.BADGE_ACCESS_LAST_SYNC_EPOCH_MS)
    } ?: 0L)
    private val badgeDialogDismissed = MutableStateFlow(defaults.boolForKey(AppSettingsKeys.BADGE_FEATURES_DIALOG_DISMISSED))

    override fun getTheme(): Flow<AppTheme> = theme
    override suspend fun saveTheme(darkThemeEnabled: String) {
        defaults.setObject(darkThemeEnabled, AppSettingsKeys.DARK_THEME_SETTINGS)
        theme.value = AppTheme.valueOf(darkThemeEnabled)
    }

    override fun getLanguage(): Flow<AppLanguage> = language
    override fun hasLanguageSelection(): Flow<Boolean> = hasLanguage
    override fun hasCompletedLanguageOnboarding(): Flow<Boolean> = onboardingDone
    override suspend fun saveLanguage(appLanguage: AppLanguage) {
        defaults.setObject(appLanguage.name, AppSettingsKeys.LANGUAGE)
        language.value = appLanguage
        hasLanguage.value = true
    }

    override suspend fun setLanguageOnboardingCompleted(completed: Boolean) {
        defaults.setBool(completed, AppSettingsKeys.LANGUAGE_ONBOARDING_DONE)
        onboardingDone.value = completed
    }

    override fun getBadgeAccessEnabled(): Flow<Boolean> = badgeEnabled
    override fun getBadgeAccessLastSyncEpochMillis(): Flow<Long> = badgeLastSync
    override suspend fun saveBadgeAccessCache(hasBadge: Boolean, syncedAtEpochMillis: Long) {
        defaults.setBool(hasBadge, AppSettingsKeys.BADGE_ACCESS_ENABLED)
        defaults.setInteger(syncedAtEpochMillis, AppSettingsKeys.BADGE_ACCESS_LAST_SYNC_EPOCH_MS)
        badgeEnabled.value = hasBadge
        badgeLastSync.value = syncedAtEpochMillis
    }

    override suspend fun clearBadgeAccessCache() = saveBadgeAccessCache(false, 0L)
    override fun getBadgeFeaturesDialogDismissed(): Flow<Boolean> = badgeDialogDismissed
    override suspend fun setBadgeFeaturesDialogDismissed(dismissed: Boolean) {
        defaults.setBool(dismissed, AppSettingsKeys.BADGE_FEATURES_DIALOG_DISMISSED)
        badgeDialogDismissed.value = dismissed
    }

    private fun readTheme(): AppTheme = defaults.stringForKey(AppSettingsKeys.DARK_THEME_SETTINGS)
        ?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() }
        ?: AppTheme.SystemSettings

    private fun readLanguage(): AppLanguage = defaults.stringForKey(AppSettingsKeys.LANGUAGE)
        ?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() }
        ?: AppLanguage.Farsi
}
