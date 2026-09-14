package com.dino.message.corefeature.data.spref

import kotlin.test.Test
import kotlin.test.assertEquals

class AppSettingsKeysTest {
    @Test
    fun preservesAndroidSettingsFileAndKeyNames() {
        assertEquals("settings.preferences_pb", AppSettingsKeys.SETTINGS_DATASTORE_FILE_NAME)
        assertEquals("DARK_THEME_SETTINGS", AppSettingsKeys.DARK_THEME_SETTINGS)
        assertEquals("LANGUAGE", AppSettingsKeys.LANGUAGE)
        assertEquals("LANGUAGE_ONBOARDING_DONE", AppSettingsKeys.LANGUAGE_ONBOARDING_DONE)
        assertEquals("BADGE_ACCESS_ENABLED", AppSettingsKeys.BADGE_ACCESS_ENABLED)
        assertEquals("BADGE_ACCESS_LAST_SYNC_EPOCH_MS", AppSettingsKeys.BADGE_ACCESS_LAST_SYNC_EPOCH_MS)
        assertEquals("BADGE_FEATURES_DIALOG_DISMISSED", AppSettingsKeys.BADGE_FEATURES_DIALOG_DISMISSED)
    }
}
