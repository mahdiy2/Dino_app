package com.dino.message.corefeature.data.spref

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Desktop secure storage backed by an AES-GCM encrypted file.
 *
 * The master key is stored separately with owner-only file permissions where
 * POSIX permissions are available. This protects data at rest, but cannot
 * protect against another process already running as the same OS user.
 */
class DesktopEncryptedSecureKeyValueStore(
    private val storageDirectory: Path,
    private val secureRandom: SecureRandom = SecureRandom(),
) : SecureKeyValueStore {
    private val dataFile = storageDirectory.resolve(DATA_FILE_NAME)
    private val keyFile = storageDirectory.resolve(KEY_FILE_NAME)

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
        val values = readValues().toMutableMap()
        if (values.remove(key) != null) writeValues(values)
        return this
    }

    override fun clear() {
        Files.deleteIfExists(dataFile)
    }

    override fun apply() = Unit

    private fun putEncoded(key: String, value: String) {
        val values = readValues().toMutableMap()
        values[key] = value
        writeValues(values)
    }

    private fun getEncoded(key: String): String? = readValues()[key]

    private fun readValues(): Map<String, String> {
        if (!Files.exists(dataFile)) return emptyMap()
        val payload = Files.readAllBytes(dataFile)
        require(payload.size > HEADER_SIZE) { "Secure storage payload is corrupt" }
        require(payload[0] == FORMAT_VERSION) { "Unsupported secure storage format" }

        val iv = payload.copyOfRange(1, HEADER_SIZE)
        val ciphertext = payload.copyOfRange(HEADER_SIZE, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(readOrCreateMasterKey(), AES), GCMParameterSpec(TAG_BITS, iv))
        val plaintext = cipher.doFinal(ciphertext).toString(StandardCharsets.UTF_8)
        return Json.parseToJsonElement(plaintext).let { element ->
            require(element is JsonObject) { "Secure storage payload is corrupt" }
            element.mapValues { (_, value) -> value.jsonPrimitive.content }
        }
    }

    private fun writeValues(values: Map<String, String>) {
        Files.createDirectories(storageDirectory)
        applyOwnerOnlyPermissions(storageDirectory, isDirectory = true)

        val plaintext = JsonObject(values.mapValues { JsonPrimitive(it.value) })
            .toString()
            .toByteArray(StandardCharsets.UTF_8)
        val iv = ByteArray(IV_BYTES).also(secureRandom::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(readOrCreateMasterKey(), AES), GCMParameterSpec(TAG_BITS, iv))
        val payload = byteArrayOf(FORMAT_VERSION) + iv + cipher.doFinal(plaintext)

        val temporaryFile = Files.createTempFile(storageDirectory, ".secure-store-", ".tmp")
        try {
            Files.write(temporaryFile, payload)
            applyOwnerOnlyPermissions(temporaryFile, isDirectory = false)
            try {
                Files.move(
                    temporaryFile,
                    dataFile,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: UnsupportedOperationException) {
                Files.move(temporaryFile, dataFile, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporaryFile)
        }
    }

    private fun readOrCreateMasterKey(): ByteArray {
        if (Files.exists(keyFile)) {
            return Files.readAllBytes(keyFile).also {
                require(it.size == KEY_BYTES) { "Secure storage master key is corrupt" }
            }
        }

        Files.createDirectories(storageDirectory)
        applyOwnerOnlyPermissions(storageDirectory, isDirectory = true)
        val key = ByteArray(KEY_BYTES).also(secureRandom::nextBytes)
        val temporaryFile = Files.createTempFile(storageDirectory, ".secure-key-", ".tmp")
        try {
            Files.write(temporaryFile, key)
            applyOwnerOnlyPermissions(temporaryFile, isDirectory = false)
            try {
                Files.move(temporaryFile, keyFile, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: UnsupportedOperationException) {
                Files.move(temporaryFile, keyFile)
            }
        } finally {
            Files.deleteIfExists(temporaryFile)
        }
        return key
    }

    private fun applyOwnerOnlyPermissions(path: Path, isDirectory: Boolean) {
        runCatching {
            Files.setPosixFilePermissions(
                path,
                PosixFilePermissions.fromString(if (isDirectory) "rwx------" else "rw-------"),
            )
        }
    }

    private companion object {
        const val DATA_FILE_NAME = "secure-store.bin"
        const val KEY_FILE_NAME = "secure-store.key"
        const val STRING_PREFIX = "s:"
        const val BOOLEAN_PREFIX = "b:"
        const val LONG_PREFIX = "l:"
        const val AES = "AES"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_BYTES = 32
        const val IV_BYTES = 12
        const val TAG_BITS = 128
        const val HEADER_SIZE = 1 + IV_BYTES
        const val FORMAT_VERSION: Byte = 1
    }
}
