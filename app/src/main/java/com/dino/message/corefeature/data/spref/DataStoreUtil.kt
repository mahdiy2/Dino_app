package com.dino.message.corefeature.data.spref

import android.content.Context
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.domain.model.AppTheme
import kotlinx.coroutines.flow.map


/**
 * The `DataStoreUtil` class provides utility methods for working with DataStore in Android applications.
 *
 * @param context The `Context` object used for accessing resources.
 */
class DataStoreUtil @Inject constructor(private val context: Context) {
    companion object {
        // Define the DataStore with the specified name
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

        // Define keys for accessing specific preferences
        val DARK_THEME_SETTINGS_KEY = stringPreferencesKey("DARK_THEME_SETTINGS")
        val LANGUAGE_KEY = stringPreferencesKey("LANGUAGE")
        val LANGUAGE_ONBOARDING_DONE_KEY = booleanPreferencesKey("LANGUAGE_ONBOARDING_DONE")
        val BADGE_ACCESS_ENABLED_KEY = booleanPreferencesKey("BADGE_ACCESS_ENABLED")
        val BADGE_ACCESS_LAST_SYNC_EPOCH_MS_KEY = longPreferencesKey("BADGE_ACCESS_LAST_SYNC_EPOCH_MS")
        val BADGE_FEATURES_DIALOG_DISMISSED_KEY = booleanPreferencesKey("BADGE_FEATURES_DIALOG_DISMISSED")
    }

    /**
     * Retrieves the theme preference from the DataStore as a flow of `AppTheme`.
     *
     * @return A `Flow` emitting the current theme preference.
     */
    fun getTheme(): Flow<AppTheme> = context.dataStore.data.map { preferences ->
        AppTheme.valueOf(
            preferences[DARK_THEME_SETTINGS_KEY] ?: AppTheme.SystemSettings.name
        )
    }

    /**
     * Saves the theme preference to the DataStore.
     *
     * @param darkThemeEnabled The value representing the dark theme preference.
     */
    suspend fun saveTheme(darkThemeEnabled: String) {
        context.dataStore.edit { preferences ->
            preferences[DARK_THEME_SETTINGS_KEY] = darkThemeEnabled
        }
    }

    /**
     * Retrieves the language preference from the DataStore as a flow of `AppLanguage`.
     *
     * @return A `Flow` emitting the current language preference.
     */
    fun getLanguage(): Flow<AppLanguage> = context.dataStore.data.map { preferences ->
        AppLanguage.valueOf(
            preferences[LANGUAGE_KEY] ?: AppLanguage.Farsi.name
        )
    }

    /**
     * Returns whether the user has explicitly selected an app language before.
     */
    fun hasLanguageSelection(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY] != null
    }

    /**
     * Returns whether the splash language prompt has been completed.
     * Default is false so existing installs also see it once after update.
     */
    fun hasCompletedLanguageOnboarding(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[LANGUAGE_ONBOARDING_DONE_KEY] ?: false
    }

    /**
     * Saves the language preference to the DataStore.
     *
     * @param appLanguage The `AppLanguage` representing the language preference.
     */
    suspend fun saveLanguage(appLanguage: AppLanguage) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = appLanguage.name
        }
    }

    /**
     * Marks the splash language onboarding as completed.
     */
    suspend fun setLanguageOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_ONBOARDING_DONE_KEY] = completed
        }
    }

    fun getBadgeAccessEnabled(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BADGE_ACCESS_ENABLED_KEY] ?: false
    }

    fun getBadgeAccessLastSyncEpochMillis(): Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[BADGE_ACCESS_LAST_SYNC_EPOCH_MS_KEY] ?: 0L
    }

    suspend fun saveBadgeAccessCache(hasBadge: Boolean, syncedAtEpochMillis: Long = System.currentTimeMillis()) {
        context.dataStore.edit { preferences ->
            preferences[BADGE_ACCESS_ENABLED_KEY] = hasBadge
            preferences[BADGE_ACCESS_LAST_SYNC_EPOCH_MS_KEY] = syncedAtEpochMillis
        }
    }

    suspend fun clearBadgeAccessCache() {
        context.dataStore.edit { preferences ->
            preferences[BADGE_ACCESS_ENABLED_KEY] = false
            preferences[BADGE_ACCESS_LAST_SYNC_EPOCH_MS_KEY] = 0L
        }
    }

    fun getBadgeFeaturesDialogDismissed(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BADGE_FEATURES_DIALOG_DISMISSED_KEY] ?: false
    }

    suspend fun setBadgeFeaturesDialogDismissed(dismissed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BADGE_FEATURES_DIALOG_DISMISSED_KEY] = dismissed
        }
    }
}
