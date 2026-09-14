package com.dino.message.corefeature.data.spref

import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.domain.model.AppTheme
import kotlinx.browser.window
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class WebStorageTest {
    private val secureNamespace = "secure-test"
    private val settingsNamespace = "settings-test"

    @AfterTest
    fun cleanUp() {
        WebSessionSecureKeyValueStore(secureNamespace).clear()
        listOf(
            AppSettingsKeys.DARK_THEME_SETTINGS,
            AppSettingsKeys.LANGUAGE,
            AppSettingsKeys.LANGUAGE_ONBOARDING_DONE,
            AppSettingsKeys.BADGE_ACCESS_ENABLED,
            AppSettingsKeys.BADGE_ACCESS_LAST_SYNC_EPOCH_MS,
            AppSettingsKeys.BADGE_FEATURES_DIALOG_DISMISSED,
        ).forEach { window.localStorage.removeItem("$settingsNamespace:$it") }
        window.sessionStorage.removeItem("unrelated")
    }

    @Test
    fun secureStoreIsTypedAndClearsOnlyItsNamespace() {
        window.sessionStorage.setItem("unrelated", "keep")
        val store = WebSessionSecureKeyValueStore(secureNamespace)
        store.putString("TOKEN", "access")
        store.putBoolean("USER_LOG_IN", true)
        store.putLong("EXPIRY", 42L)

        assertEquals("access", store.getString("TOKEN"))
        assertEquals(true, store.getBoolean("USER_LOG_IN", false))
        assertEquals(42L, store.getLong("EXPIRY", 0L))

        store.clear()
        assertNull(store.getString("TOKEN"))
        assertEquals("keep", window.sessionStorage.getItem("unrelated"))
    }

    @Test
    fun settingsUseExistingDefaultsAndPersistInLocalStorage() = runTest {
        val store = WebAppSettingsStore(settingsNamespace)
        assertEquals(AppTheme.SystemSettings, store.getTheme().first())
        assertEquals(AppLanguage.Farsi, store.getLanguage().first())
        assertFalse(store.hasLanguageSelection().first())

        store.saveTheme(AppTheme.DarkTheme.name)
        store.saveLanguage(AppLanguage.English)
        store.setLanguageOnboardingCompleted(true)

        val reopened = WebAppSettingsStore(settingsNamespace)
        assertEquals(AppTheme.DarkTheme, reopened.getTheme().first())
        assertEquals(AppLanguage.English, reopened.getLanguage().first())
        assertEquals(true, reopened.hasCompletedLanguageOnboarding().first())
    }
}
