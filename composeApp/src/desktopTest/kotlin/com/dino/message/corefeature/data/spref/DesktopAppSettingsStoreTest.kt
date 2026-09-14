package com.dino.message.corefeature.data.spref

import com.dino.message.corefeature.domain.model.AppLanguage
import com.dino.message.corefeature.domain.model.AppTheme
import java.util.prefs.AbstractPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopAppSettingsStoreTest {
    @Test
    fun preservesDefaultsAndPersistsEverySetting() = runTest {
        val preferences = MemoryPreferences()
        val store = DesktopAppSettingsStore(preferences)

        assertEquals(AppTheme.SystemSettings, store.getTheme().first())
        assertEquals(AppLanguage.Farsi, store.getLanguage().first())
        assertFalse(store.hasLanguageSelection().first())
        assertFalse(store.hasCompletedLanguageOnboarding().first())
        assertFalse(store.getBadgeAccessEnabled().first())
        assertEquals(0L, store.getBadgeAccessLastSyncEpochMillis().first())
        assertFalse(store.getBadgeFeaturesDialogDismissed().first())

        store.saveTheme(AppTheme.DarkTheme.name)
        store.saveLanguage(AppLanguage.English)
        store.setLanguageOnboardingCompleted(true)
        store.saveBadgeAccessCache(hasBadge = true, syncedAtEpochMillis = 123L)
        store.setBadgeFeaturesDialogDismissed(true)

        val reopened = DesktopAppSettingsStore(preferences)
        assertEquals(AppTheme.DarkTheme, reopened.getTheme().first())
        assertEquals(AppLanguage.English, reopened.getLanguage().first())
        assertTrue(reopened.hasLanguageSelection().first())
        assertTrue(reopened.hasCompletedLanguageOnboarding().first())
        assertTrue(reopened.getBadgeAccessEnabled().first())
        assertEquals(123L, reopened.getBadgeAccessLastSyncEpochMillis().first())
        assertTrue(reopened.getBadgeFeaturesDialogDismissed().first())

        reopened.clearBadgeAccessCache()
        assertFalse(reopened.getBadgeAccessEnabled().first())
        assertEquals(0L, reopened.getBadgeAccessLastSyncEpochMillis().first())
    }

    private class MemoryPreferences(
        parent: AbstractPreferences? = null,
        name: String = "",
    ) : AbstractPreferences(parent, name) {
        private val values = mutableMapOf<String, String>()
        private val children = mutableMapOf<String, MemoryPreferences>()

        override fun putSpi(key: String, value: String) {
            values[key] = value
        }

        override fun getSpi(key: String): String? = values[key]

        override fun removeSpi(key: String) {
            values.remove(key)
        }

        override fun removeNodeSpi() {
            values.clear()
            children.clear()
        }

        override fun keysSpi(): Array<String> = values.keys.toTypedArray()

        override fun childrenNamesSpi(): Array<String> = children.keys.toTypedArray()

        override fun childSpi(name: String): AbstractPreferences =
            children.getOrPut(name) { MemoryPreferences(this, name) }

        override fun syncSpi() = Unit

        override fun flushSpi() = Unit
    }
}
