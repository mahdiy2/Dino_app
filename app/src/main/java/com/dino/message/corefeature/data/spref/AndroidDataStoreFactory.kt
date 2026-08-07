package com.dino.message.corefeature.data.spref

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

fun createSettingsDataStore(context: Context): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath {
        context.filesDir
            .resolve("datastore")
            .resolve(DataStoreUtil.SETTINGS_DATASTORE_FILE_NAME)
            .absolutePath
            .toPath()
    }
