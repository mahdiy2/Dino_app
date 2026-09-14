package com.dino.message.corefeature.data.spref

import kotlinx.browser.window

/**
 * Browser storage for secrets. Values live only for the current tab session.
 * This is not resistant to XSS; authenticated web flows must not be considered
 * production-hardened until secrets are backed by a stronger design.
 */
class WebSessionSecureKeyValueStore(
    private val namespace: String = DEFAULT_NAMESPACE,
) : SecureKeyValueStore {
    override fun putString(key: String, value: String) {
        window.sessionStorage.setItem(namespaced(key), STRING_PREFIX + value)
    }

    override fun getString(key: String, defaultValue: String?): String? =
        window.sessionStorage.getItem(namespaced(key))
            ?.takeIf { it.startsWith(STRING_PREFIX) }
            ?.removePrefix(STRING_PREFIX)
            ?: defaultValue

    override fun putBoolean(key: String, value: Boolean) {
        window.sessionStorage.setItem(namespaced(key), BOOLEAN_PREFIX + value)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        window.sessionStorage.getItem(namespaced(key))
            ?.takeIf { it.startsWith(BOOLEAN_PREFIX) }
            ?.removePrefix(BOOLEAN_PREFIX)
            ?.toBooleanStrictOrNull()
            ?: defaultValue

    override fun putLong(key: String, value: Long) {
        window.sessionStorage.setItem(namespaced(key), LONG_PREFIX + value)
    }

    override fun getLong(key: String, defaultValue: Long): Long =
        window.sessionStorage.getItem(namespaced(key))
            ?.takeIf { it.startsWith(LONG_PREFIX) }
            ?.removePrefix(LONG_PREFIX)
            ?.toLongOrNull()
            ?: defaultValue

    override fun remove(key: String): SecureKeyValueStore {
        window.sessionStorage.removeItem(namespaced(key))
        return this
    }

    override fun clear() {
        val keys = buildList {
            repeat(window.sessionStorage.length) { index ->
                window.sessionStorage.key(index)?.takeIf { it.startsWith(namespacePrefix) }?.let(::add)
            }
        }
        keys.forEach(window.sessionStorage::removeItem)
    }

    override fun apply() = Unit

    private fun namespaced(key: String): String = namespacePrefix + key

    private val namespacePrefix: String
        get() = "$namespace:"

    private companion object {
        const val DEFAULT_NAMESPACE = "com.sec.message.secure"
        const val STRING_PREFIX = "s:"
        const val BOOLEAN_PREFIX = "b:"
        const val LONG_PREFIX = "l:"
    }
}
