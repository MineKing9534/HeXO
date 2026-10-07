package de.mineking.hexo.discord.oauth2.api

import de.mineking.hexo.discord.oauth2.model.OAuth2Flow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes

class OAuth2AuthorizationSessionStoreTest {
    private val store = InMemoryOAuth2AuthorizationSessionStore(
        expireAfter = 5.minutes,
        maxEntries = 10,
    )

    @Test
    fun `authorization state can only be consumed by its browser`() = runTest {
        val state = store.create(OAuth2Flow.WebLogin, "browser-1")

        assertNull(store.consume(state, "browser-2"))
        assertEquals(OAuth2Flow.WebLogin, store.consume(state, "browser-1")?.flow)
        assertNull(store.consume(state, "browser-1"))
    }
}
