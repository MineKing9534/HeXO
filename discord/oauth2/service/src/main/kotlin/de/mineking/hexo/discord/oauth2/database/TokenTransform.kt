package de.mineking.hexo.discord.oauth2.database

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface TokenTransform {
    fun wrap(value: String): ByteArray
    fun unwrap(value: ByteArray): String
}

class AESTokenTransform(private val key: SecretKey) : TokenTransform {
    private val random = SecureRandom()

    override fun wrap(value: String): ByteArray {
        val iv = ByteArray(IV_SIZE)
        random.nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_SIZE_BITS, iv))
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))

        return ByteBuffer.allocate(iv.size + encrypted.size)
            .put(iv)
            .put(encrypted)
            .array()
    }

    override fun unwrap(value: ByteArray): String {
        require(value.size > IV_SIZE) { "Invalid encrypted payload." }

        val buffer = ByteBuffer.wrap(value)
        val iv = ByteArray(IV_SIZE)
        buffer.get(iv)

        val encrypted = ByteArray(buffer.remaining())
        buffer.get(encrypted)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_SIZE_BITS, iv))
        val decrypted = cipher.doFinal(encrypted)

        return decrypted.toString(StandardCharsets.UTF_8)
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_SIZE_BITS = 128
    }
}
