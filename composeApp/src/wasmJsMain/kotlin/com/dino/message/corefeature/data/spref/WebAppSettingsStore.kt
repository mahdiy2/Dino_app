package com.dino.message.corefeature.data.spref

import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.domain.model.AppTheme
import com.dino.message.corefeature.domain.settings.AppSettingsStore
import kotlinx.browser.window
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class WebAppSettingsStore(
    private val namespace: String = DEFAULT_NAMESPACE,
) : AppSettingsStore {
    private val theme = MutableStateFlow(enumValue(key(AppSettingsKeys.DARK_THEME_SETTINGS), AppTheme.SystemSettings))
    private val language = MutableStateFlow(enumValue(key(AppSettingsKeys.LANGUAGE), AppLanguage.Farsi))
    private val hasLanguage = MutableStateFlow(window.localStorage.getItem(key(AppSettingsKeys.LANGUAGE)) != null)
    private val onboardingDone = MutableStateFlow(booleanValue(AppSettingsKeys.LANGUAGE_ONBOARDING_DONE, false))
    private val badgeEnabled = MutableStateFlow(booleanValue(AppSettingsKeys.BADGE_ACCESS_ENABLED, false))
    private val badgeLastSync = MutableStateFlow(longValue(AppSettingsKeys.BADGE_ACCESS_LAST_SYNC_EPOCH_MS, 0L))
    private val badgeDialogDismissed = MutableStateFlow(booleanValue(AppSettingsKeys.BADGE_FEATURES_DIALOG_DISMISSED, false))

    override fun getTheme(): Flow<AppTheme> = theme
    override suspend fun saveTheme(darkThemeEnabled: String) {
        window.localStorage.setItem(key(AppSettingsKeys.DARK_THEME_SETTINGS), darkThemeEnabled)
        theme.value = AppTheme.valueOf(darkThemeEnabled)
    }

    override fun getLanguage(): Flow<AppLanguage> = language
    override fun hasLanguageSelection(): Flow<Boolean> = hasLanguage
    override fun hasCompletedLanguageOnboarding(): Flow<Boolean> = onboardingDone
    override suspend fun saveLanguage(appLanguage: AppLanguage) {
        window.localStorage.setItem(key(AppSettingsKeys.LANGUAGE), appLanguage.name)
        language.value = appLanguage
        hasLanguage.value = true
    }

    override suspend fun setLanguageOnboardingCompleted(completed: Boolean) {
        window.localStorage.setItem(key(AppSettingsKeys.LANGUAGE_ONBOARDING_DONE), completed.toString())
        onboardingDone.value = completed
    }

    override fun getBadgeAccessEnabled(): Flow<Boolean> = badgeEnabled
    override fun getBadgeAccessLastSyncEpochMillis(): Flow<Long> = badgeLastSync
    override suspend fun saveBadgeAccessCache(hasBadge: Boolean, syncedAtEpochMillis: Long) {
        window.localStorage.setItem(key(AppSettingsKeys.BADGE_ACCESS_ENABLED), hasBadge.toString())
        window.localStorage.setItem(key(AppSettingsKeys.BADGE_ACCESS_LAST_SYNC_EPOCH_MS), syncedAtEpochMillis.toString())
        badgeEnabled.value = hasBadge
        badgeLastSync.value = syncedAtEpochMillis
    }

    override suspend fun clearBadgeAccessCache() = saveBadgeAccessCache(false, 0L)
    override fun getBadgeFeaturesDialogDismissed(): Flow<Boolean> = badgeDialogDismissed
    override suspend fun setBadgeFeaturesDialogDismissed(dismissed: Boolean) {
        window.localStorage.setItem(key(AppSettingsKeys.BADGE_FEATURES_DIALOG_DISMISSED), dismissed.toString())
        badgeDialogDismissed.value = dismissed
    }

    private fun key(raw: String): String = "$namespace:$raw"
    private fun booleanValue(raw: String, default: Boolean): Boolean =
        window.localStorage.getItem(key(raw))?.toBooleanStrictOrNull() ?: default
    private fun longValue(raw: String, default: Long): Long =
        window.localStorage.getItem(key(raw))?.toLongOrNull() ?: default
    private inline fun <reified T : Enum<T>> enumValue(key: String, default: T): T =
        window.localStorage.getItem(key)?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

    private companion object {
        const val DEFAULT_NAMESPACE = "com.sec.message.settings"
    }
}
