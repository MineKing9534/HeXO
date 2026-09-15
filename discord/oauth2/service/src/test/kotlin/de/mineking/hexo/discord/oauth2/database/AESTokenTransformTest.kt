package de.mineking.hexo.discord.oauth2.database

import javax.crypto.spec.SecretKeySpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class AESTokenTransformTest {
    private val key = SecretKeySpec(ByteArray(32) { it.toByte() }, "AES")
    private val transform = AESTokenTransform(key)

    @Test
    fun `round trips a context-bound token`() {
        val encrypted = transform.wrap("secret-token", "user-1:access")

        assertEquals("secret-token", transform.unwrap(encrypted, "user-1:access"))
    }

    @Test
    fun `rejects a token moved to another context`() {
        val encrypted = transform.wrap("secret-token", "user-1:access")

        assertFails { transform.unwrap(encrypted, "user-2:access") }
        assertFails { transform.unwrap(encrypted, "user-1:refresh") }
    }

    @Test
    fun `access and refresh tokens use their respective contexts`() {
        val accessToken = transform.wrap("access-token", "discord-oauth2:1:access-token")
        val refreshToken = transform.wrap("refresh-token", "discord-oauth2:1:refresh-token")

        assertEquals("access-token", transform.unwrap(accessToken, "discord-oauth2:1:access-token"))
        assertEquals("refresh-token", transform.unwrap(refreshToken, "discord-oauth2:1:refresh-token"))
    }
}
