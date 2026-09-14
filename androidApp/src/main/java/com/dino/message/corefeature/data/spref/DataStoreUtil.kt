package com.dino.message.corefeature.data.spref

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.domain.model.AppTheme
import com.dino.message.corefeature.domain.settings.AppSettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


/**
 * The `DataStoreUtil` class provides utility methods for working with DataStore.
 *
 * @param dataStore The preferences DataStore supplied by the platform layer.
 */
class DataStoreUtil constructor(private val dataStore: DataStore<Preferences>) : AppSettingsStore {
    companion object {
        const val SETTINGS_DATASTORE_FILE_NAME = AppSettingsKeys.SETTINGS_DATASTORE_FILE_NAME

        // Define keys for accessing specific preferences
        val DARK_THEME_SETTINGS_KEY = stringPreferencesKey(AppSettingsKeys.DARK_THEME_SETTINGS)
        val LANGUAGE_KEY = stringPreferencesKey(AppSettingsKeys.LANGUAGE)
        val LANGUAGE_ONBOARDING_DONE_KEY = booleanPreferencesKey(AppSettingsKeys.LANGUAGE_ONBOARDING_DONE)
        val BADGE_ACCESS_ENABLED_KEY = booleanPreferencesKey(AppSettingsKeys.BADGE_ACCESS_ENABLED)
        val BADGE_ACCESS_LAST_SYNC_EPOCH_MS_KEY = longPreferencesKey(AppSettingsKeys.BADGE_ACCESS_LAST_SYNC_EPOCH_MS)
        val BADGE_FEATURES_DIALOG_DISMISSED_KEY = booleanPreferencesKey(AppSettingsKeys.BADGE_FEATURES_DIALOG_DISMISSED)
    }

    /**
     * Retrieves the theme preference from the DataStore as a flow of `AppTheme`.
     *
     * @return A `Flow` emitting the current theme preference.
     */
    override fun getTheme(): Flow<AppTheme> = dataStore.data.map { preferences ->
        AppTheme.valueOf(
            preferences[DARK_THEME_SETTINGS_KEY] ?: AppTheme.SystemSettings.name
        )
    }

    /**
     * Saves the theme preference to the DataStore.
     *
     * @param darkThemeEnabled The value representing the dark theme preference.
     */
    override suspend fun saveTheme(darkThemeEnabled: String) {
        dataStore.edit { preferences ->
            preferences[DARK_THEME_SETTINGS_KEY] = darkThemeEnabled
        }
    }

    /**
     * Retrieves the language preference from the DataStore as a flow of `AppLanguage`.
     *
     * @return A `Flow` emitting the current language preference.
     */
    override fun getLanguage(): Flow<AppLanguage> = dataStore.data.map { preferences ->
        AppLanguage.valueOf(
            preferences[LANGUAGE_KEY] ?: AppLanguage.Farsi.name
        )
    }

    /**
     * Returns whether the user has explicitly selected an app language before.
     */
    override fun hasLanguageSelection(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY] != null
    }

    /**
     * Returns whether the splash language prompt has been completed.
     * Default is false so existing installs also see it once after update.
     */
    override fun hasCompletedLanguageOnboarding(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[LANGUAGE_ONBOARDING_DONE_KEY] ?: false
    }

    /**
     * Saves the language preference to the DataStore.
     *
     * @param appLanguage The `AppLanguage` representing the language preference.
     */
    override suspend fun saveLanguage(appLanguage: AppLanguage) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = appLanguage.name
        }
    }

    /**
     * Marks the splash language onboarding as completed.
     */
    override suspend fun setLanguageOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[LANGUAGE_ONBOARDING_DONE_KEY] = completed
        }
    }

    override fun getBadgeAccessEnabled(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[BADGE_ACCESS_ENABLED_KEY] ?: false
    }

    override fun getBadgeAccessLastSyncEpochMillis(): Flow<Long> = dataStore.data.map { preferences ->
        preferences[BADGE_ACCESS_LAST_SYNC_EPOCH_MS_KEY] ?: 0L
    }

    override suspend fun saveBadgeAccessCache(hasBadge: Boolean, syncedAtEpochMillis: Long) {
        dataStore.edit { preferences ->
            preferences[BADGE_ACCESS_ENABLED_KEY] = hasBadge
            preferences[BADGE_ACCESS_LAST_SYNC_EPOCH_MS_KEY] = syncedAtEpochMillis
        }
    }

    override suspend fun clearBadgeAccessCache() {
        dataStore.edit { preferences ->
            preferences[BADGE_ACCESS_ENABLED_KEY] = false
            preferences[BADGE_ACCESS_LAST_SYNC_EPOCH_MS_KEY] = 0L
        }
    }

    override fun getBadgeFeaturesDialogDismissed(): Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[BADGE_FEATURES_DIALOG_DISMISSED_KEY] ?: false
    }

    override suspend fun setBadgeFeaturesDialogDismissed(dismissed: Boolean) {
        dataStore.edit { preferences ->
            preferences[BADGE_FEATURES_DIALOG_DISMISSED_KEY] = dismissed
        }
    }
}
