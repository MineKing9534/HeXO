package de.mineking.hexo.discord.oauth2.database

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface TokenTransform {
    fun wrap(value: String, context: String): ByteArray
    fun unwrap(value: ByteArray, context: String): String
}

class AESTokenTransform(private val key: SecretKey) : TokenTransform {
    private val random = SecureRandom()

    override fun wrap(value: String, context: String): ByteArray {
        val iv = ByteArray(IV_SIZE)
        random.nextBytes(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_SIZE_BITS, iv))
        cipher.updateAAD(context.toByteArray(StandardCharsets.UTF_8))
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))

        return ByteBuffer.allocate(FORMAT_MAGIC.size + iv.size + encrypted.size)
            .put(FORMAT_MAGIC)
            .put(iv)
            .put(encrypted)
            .array()
    }

    override fun unwrap(value: ByteArray, context: String): String {
        require(value.size > IV_SIZE + FORMAT_MAGIC.size) { "Invalid encrypted payload." }
        require(value.copyOfRange(0, FORMAT_MAGIC.size).contentEquals(FORMAT_MAGIC)) { "Unsupported encrypted payload format." }

        val buffer = ByteBuffer.wrap(value)
        buffer.position(FORMAT_MAGIC.size)

        val iv = ByteArray(IV_SIZE)
        buffer.get(iv)

        val encrypted = ByteArray(buffer.remaining())
        buffer.get(encrypted)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_SIZE_BITS, iv))
        cipher.updateAAD(context.toByteArray(StandardCharsets.UTF_8))
        val decrypted = cipher.doFinal(encrypted)

        return decrypted.toString(StandardCharsets.UTF_8)
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_SIZE_BITS = 128
        val FORMAT_MAGIC = byteArrayOf(0x48, 0x58, 0x4f, 0x01)
    }
}
