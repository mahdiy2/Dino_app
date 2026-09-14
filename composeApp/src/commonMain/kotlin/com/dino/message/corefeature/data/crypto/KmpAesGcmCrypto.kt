package com.dino.message.corefeature.data.crypto

import com.dino.message.corefeature.domain.crypto.AesGcmCrypto
import dev.whyoleg.cryptography.BinarySize.Companion.bits
import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.DelicateCryptographyApi
import dev.whyoleg.cryptography.algorithms.AES

@OptIn(DelicateCryptographyApi::class)
class KmpAesGcmCrypto(
    private val provider: CryptographyProvider = CryptographyProvider.Default,
) : AesGcmCrypto {
    override suspend fun generateKey(sizeBytes: Int): ByteArray {
        require(sizeBytes in setOf(16, 24, 32)) { "Unsupported AES key size" }
        return provider.get(AES.GCM)
            .keyGenerator((sizeBytes * 8).bits)
            .generateKey()
            .encodeToByteArray(AES.Key.Format.RAW)
    }

    override suspend fun encrypt(
        plainText: ByteArray,
        key: ByteArray,
        ivSizeBytes: Int,
        tagLengthBits: Int,
    ): ByteArray {
        require(ivSizeBytes == GCM_IV_SIZE_BYTES) { "AES-GCM requires a 12-byte IV" }
        return gcmKey(key).cipher(tagLengthBits.bits).encrypt(plainText)
    }

    override suspend fun decrypt(
        encryptedPayload: ByteArray,
        key: ByteArray,
        ivSizeBytes: Int,
        tagLengthBits: Int,
    ): ByteArray {
        require(ivSizeBytes == GCM_IV_SIZE_BYTES) { "AES-GCM requires a 12-byte IV" }
        require(encryptedPayload.size > ivSizeBytes) { "Invalid encrypted payload" }
        return gcmKey(key).cipher(tagLengthBits.bits).decrypt(encryptedPayload)
    }

    override suspend fun encryptWithIv(
        plainText: ByteArray,
        key: ByteArray,
        iv: ByteArray,
        tagLengthBits: Int,
    ): ByteArray {
        require(iv.size == GCM_IV_SIZE_BYTES) { "AES-GCM requires a 12-byte IV" }
        val ciphertext = gcmKey(key).cipher(tagLengthBits.bits).encryptWithIv(iv, plainText)
        return iv + ciphertext
    }

    override suspend fun decryptLegacyEcb(encryptedPayload: ByteArray, key: ByteArray): ByteArray {
        provider.getOrNull(AES.ECB)?.let { algorithm ->
            val aesKey = algorithm.keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, key)
            return aesKey.cipher(padding = true).decrypt(encryptedPayload)
        }
        return decryptLegacyEcbViaCbc(encryptedPayload, key)
    }

    private suspend fun decryptLegacyEcbViaCbc(encryptedPayload: ByteArray, key: ByteArray): ByteArray {
        require(encryptedPayload.isNotEmpty() && encryptedPayload.size % AES_BLOCK_BYTES == 0) {
            "Invalid legacy AES payload"
        }
        val cbc = provider.get(AES.CBC)
        val aesKey = cbc.keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, key)
        val cipher = aesKey.cipher(padding = true)
        val zeroIv = ByteArray(AES_BLOCK_BYTES)
        val paddingBlock = ByteArray(AES_BLOCK_BYTES) { AES_BLOCK_BYTES.toByte() }
        val plaintext = ByteArray(encryptedPayload.size)

        for (offset in encryptedPayload.indices step AES_BLOCK_BYTES) {
            val ciphertextBlock = encryptedPayload.copyOfRange(offset, offset + AES_BLOCK_BYTES)
            val paddingInput = ByteArray(AES_BLOCK_BYTES) { index ->
                (ciphertextBlock[index].toInt() xor paddingBlock[index].toInt()).toByte()
            }
            val paddingCiphertext = cipher.encryptWithIv(zeroIv, paddingInput).copyOfRange(0, AES_BLOCK_BYTES)
            val decryptedBlock = cipher.decryptWithIv(zeroIv, ciphertextBlock + paddingCiphertext)
            require(decryptedBlock.size == AES_BLOCK_BYTES) { "Invalid legacy AES block" }
            decryptedBlock.copyInto(plaintext, destinationOffset = offset)
        }

        val paddingSize = plaintext.last().toInt() and 0xff
        require(paddingSize in 1..AES_BLOCK_BYTES &&
            plaintext.takeLast(paddingSize).all { (it.toInt() and 0xff) == paddingSize }
        ) { "Invalid legacy AES padding" }
        return plaintext.copyOf(plaintext.size - paddingSize)
    }

    private suspend fun gcmKey(key: ByteArray): AES.GCM.Key = provider.get(AES.GCM)
        .keyDecoder()
        .decodeFromByteArray(AES.Key.Format.RAW, key)

    private companion object {
        const val GCM_IV_SIZE_BYTES = 12
        const val AES_BLOCK_BYTES = 16
    }
}
