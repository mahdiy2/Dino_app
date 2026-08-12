package com.dino.message.corefeature.data.spref

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.dino.message.corefeature.data.util.Constants.SEC_MESSAGE_SHARED_PREFERENCES_NAME

class AndroidEncryptedSecureKeyValueStore(
    context: Context
) : SecureKeyValueStore {
    private val masterKey: MasterKey =
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        SEC_MESSAGE_SHARED_PREFERENCES_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun putString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    override fun getString(key: String, defaultValue: String?): String? =
        sharedPreferences.getString(key, defaultValue)

    override fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        sharedPreferences.getBoolean(key, defaultValue)

    override fun putLong(key: String, value: Long) {
        sharedPreferences.edit().putLong(key, value).apply()
    }

    override fun getLong(key: String, defaultValue: Long): Long =
        sharedPreferences.getLong(key, defaultValue)

    override fun remove(key: String): SecureKeyValueStore {
        sharedPreferences.edit().remove(key).apply()
        return this
    }

    override fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    override fun apply() = Unit
}
