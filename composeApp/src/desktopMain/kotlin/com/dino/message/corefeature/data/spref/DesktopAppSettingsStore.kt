package com.dino.message.corefeature.data.spref

import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.domain.model.AppTheme
import com.dino.message.corefeature.domain.settings.AppSettingsStore
import java.util.prefs.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class DesktopAppSettingsStore(
    private val preferences: Preferences = Preferences.userRoot().node(DEFAULT_NODE),
) : AppSettingsStore {
    private val theme = MutableStateFlow(readTheme())
    private val language = MutableStateFlow(readLanguage())
    private val hasLanguage = MutableStateFlow(preferences.get(AppSettingsKeys.LANGUAGE, null) != null)
    private val onboardingDone = MutableStateFlow(preferences.getBoolean(AppSettingsKeys.LANGUAGE_ONBOARDING_DONE, false))
    private val badgeEnabled = MutableStateFlow(preferences.getBoolean(AppSettingsKeys.BADGE_ACCESS_ENABLED, false))
    private val badgeLastSync = MutableStateFlow(preferences.getLong(AppSettingsKeys.BADGE_ACCESS_LAST_SYNC_EPOCH_MS, 0L))
    private val badgeDialogDismissed = MutableStateFlow(preferences.getBoolean(AppSettingsKeys.BADGE_FEATURES_DIALOG_DISMISSED, false))

    override fun getTheme(): Flow<AppTheme> = theme
    override suspend fun saveTheme(darkThemeEnabled: String) {
        preferences.put(AppSettingsKeys.DARK_THEME_SETTINGS, darkThemeEnabled)
        theme.value = AppTheme.valueOf(darkThemeEnabled)
    }

    override fun getLanguage(): Flow<AppLanguage> = language
    override fun hasLanguageSelection(): Flow<Boolean> = hasLanguage
    override fun hasCompletedLanguageOnboarding(): Flow<Boolean> = onboardingDone
    override suspend fun saveLanguage(appLanguage: AppLanguage) {
        preferences.put(AppSettingsKeys.LANGUAGE, appLanguage.name)
        language.value = appLanguage
        hasLanguage.value = true
    }

    override suspend fun setLanguageOnboardingCompleted(completed: Boolean) {
        preferences.putBoolean(AppSettingsKeys.LANGUAGE_ONBOARDING_DONE, completed)
        onboardingDone.value = completed
    }

    override fun getBadgeAccessEnabled(): Flow<Boolean> = badgeEnabled
    override fun getBadgeAccessLastSyncEpochMillis(): Flow<Long> = badgeLastSync
    override suspend fun saveBadgeAccessCache(hasBadge: Boolean, syncedAtEpochMillis: Long) {
        preferences.putBoolean(AppSettingsKeys.BADGE_ACCESS_ENABLED, hasBadge)
        preferences.putLong(AppSettingsKeys.BADGE_ACCESS_LAST_SYNC_EPOCH_MS, syncedAtEpochMillis)
        badgeEnabled.value = hasBadge
        badgeLastSync.value = syncedAtEpochMillis
    }

    override suspend fun clearBadgeAccessCache() = saveBadgeAccessCache(false, 0L)

    override fun getBadgeFeaturesDialogDismissed(): Flow<Boolean> = badgeDialogDismissed
    override suspend fun setBadgeFeaturesDialogDismissed(dismissed: Boolean) {
        preferences.putBoolean(AppSettingsKeys.BADGE_FEATURES_DIALOG_DISMISSED, dismissed)
        badgeDialogDismissed.value = dismissed
    }

    private fun readTheme(): AppTheme = runCatching {
        AppTheme.valueOf(preferences.get(AppSettingsKeys.DARK_THEME_SETTINGS, AppTheme.SystemSettings.name))
    }.getOrDefault(AppTheme.SystemSettings)

    private fun readLanguage(): AppLanguage = runCatching {
        AppLanguage.valueOf(preferences.get(AppSettingsKeys.LANGUAGE, AppLanguage.Farsi.name))
    }.getOrDefault(AppLanguage.Farsi)

    private companion object {
        const val DEFAULT_NODE = "/com/sec/message/settings"
    }
}
