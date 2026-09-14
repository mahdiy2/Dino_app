package com.dino.message.corefeature.data.spref

interface SecureKeyValueStore {
    fun putString(key: String, value: String)
    fun getString(key: String, defaultValue: String? = null): String?
    fun putBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putLong(key: String, value: Long)
    fun getLong(key: String, defaultValue: Long): Long
    fun remove(key: String): SecureKeyValueStore
    fun clear()
    fun apply()
}
