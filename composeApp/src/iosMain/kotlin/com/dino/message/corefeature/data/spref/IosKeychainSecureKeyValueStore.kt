package com.dino.message.corefeature.data.spref

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecDuplicateItem
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

@OptIn(ExperimentalForeignApi::class)
class IosKeychainSecureKeyValueStore(
    private val service: String = DEFAULT_SERVICE,
) : SecureKeyValueStore {
    override fun putString(key: String, value: String) = putEncoded(key, STRING_PREFIX + value)

    override fun getString(key: String, defaultValue: String?): String? =
        getEncoded(key)?.takeIf { it.startsWith(STRING_PREFIX) }?.removePrefix(STRING_PREFIX)
            ?: defaultValue

    override fun putBoolean(key: String, value: Boolean) = putEncoded(key, BOOLEAN_PREFIX + value)

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        getEncoded(key)?.takeIf { it.startsWith(BOOLEAN_PREFIX) }
            ?.removePrefix(BOOLEAN_PREFIX)
            ?.toBooleanStrictOrNull()
            ?: defaultValue

    override fun putLong(key: String, value: Long) = putEncoded(key, LONG_PREFIX + value)

    override fun getLong(key: String, defaultValue: Long): Long =
        getEncoded(key)?.takeIf { it.startsWith(LONG_PREFIX) }
            ?.removePrefix(LONG_PREFIX)
            ?.toLongOrNull()
            ?: defaultValue

    override fun remove(key: String): SecureKeyValueStore {
        val status = SecItemDelete(baseQuery(key))
        requireStatus(status, allowNotFound = true)
        return this
    }

    override fun clear() {
        val query = NSMutableDictionary().apply {
            setObject(kSecClassGenericPassword, forKey = kSecClass)
            setObject(service, forKey = kSecAttrService)
        }
        requireStatus(SecItemDelete(query), allowNotFound = true)
    }

    override fun apply() = Unit

    private fun putEncoded(key: String, value: String) {
        remove(key)
        val query = baseQuery(key).apply {
            setObject(value.encodeToByteArray().toNSData(), forKey = kSecValueData)
            setObject(kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly, forKey = kSecAttrAccessible)
        }
        val status = SecItemAdd(query, null)
        if (status == errSecDuplicateItem) {
            remove(key)
            requireStatus(SecItemAdd(query, null))
        } else {
            requireStatus(status)
        }
    }

    private fun getEncoded(key: String): String? = memScoped {
        val query = baseQuery(key).apply {
            setObject(true, forKey = kSecReturnData)
            setObject(kSecMatchLimitOne, forKey = kSecMatchLimit)
        }
        val result = alloc<CFTypeRefVar>()
        val status = SecItemCopyMatching(query, result.ptr)
        if (status == errSecItemNotFound) return@memScoped null
        requireStatus(status)
        val data = result.value as? NSData ?: error("Keychain returned an invalid value")
        data.toByteArray().decodeToString()
    }

    private fun baseQuery(key: String): NSMutableDictionary = NSMutableDictionary().apply {
        setObject(kSecClassGenericPassword, forKey = kSecClass)
        setObject(service, forKey = kSecAttrService)
        setObject(key, forKey = kSecAttrAccount)
    }

    private fun requireStatus(status: Int, allowNotFound: Boolean = false) {
        if (status == errSecSuccess || (allowNotFound && status == errSecItemNotFound)) return
        error("Keychain operation failed with status $status")
    }

    private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }

    private fun NSData.toByteArray(): ByteArray = ByteArray(length.toInt()).also { bytes ->
        bytes.usePinned { pinned ->
            platform.posix.memcpy(pinned.addressOf(0), this.bytes, length)
        }
    }

    private companion object {
        const val DEFAULT_SERVICE = "com.sec.message.secure"
        const val STRING_PREFIX = "s:"
        const val BOOLEAN_PREFIX = "b:"
        const val LONG_PREFIX = "l:"
    }
}
