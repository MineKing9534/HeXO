package de.mineking.hexo.discord.oauth2.api

import de.mineking.hexo.discord.oauth2.model.OAuth2Flow
import de.mineking.hexo.utils.cache.CacheConfiguration
import de.mineking.hexo.utils.cache.EvictionStrategy
import de.mineking.hexo.utils.cache.ExpirationStrategy
import de.mineking.hexo.utils.cache.InMemoryCache
import de.mineking.hexo.utils.cache.entries
import java.security.SecureRandom
import kotlin.time.Duration

data class OAuth2AuthorizationSession(
    val flow: OAuth2Flow,
    val browserId: String,
)

interface OAuth2AuthorizationSessionStore {
    suspend fun create(flow: OAuth2Flow, browserId: String): String
    suspend fun consume(state: String, browserId: String): OAuth2AuthorizationSession?
}

class InMemoryOAuth2AuthorizationSessionStore(
    expireAfter: Duration,
    maxEntries: Int,
) : OAuth2AuthorizationSessionStore {
    private val random = SecureRandom()
    private val sessions = InMemoryCache(CacheConfiguration<String, OAuth2AuthorizationSession>(
        expiration = ExpirationStrategy.AfterWrite(expireAfter),
        sizeLimit = maxEntries.entries,
        evictionStrategy = EvictionStrategy.FirstInFirstOut,
    ))

    override suspend fun create(flow: OAuth2Flow, browserId: String): String {
        val state = ByteArray(16).also(random::nextBytes).toHexString()
        sessions.put(state, OAuth2AuthorizationSession(flow, browserId))
        return state
    }

    override suspend fun consume(state: String, browserId: String): OAuth2AuthorizationSession? {
        val session = sessions.get(state)?.takeIf { it.browserId == browserId } ?: return null
        return sessions.remove(state)?.takeIf { it == session }
    }
}
