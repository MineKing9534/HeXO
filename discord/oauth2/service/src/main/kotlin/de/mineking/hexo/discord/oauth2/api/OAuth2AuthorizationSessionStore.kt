package de.mineking.hexo.discord.oauth2.api

import de.mineking.hexo.discord.oauth2.model.OAuth2Flow
import de.mineking.hexo.utils.cache.CacheConfiguration
import de.mineking.hexo.utils.cache.EvictionStrategy
import de.mineking.hexo.utils.cache.ExpirationStrategy
import de.mineking.hexo.utils.cache.InMemoryCache
import de.mineking.hexo.utils.cache.entries
import java.security.SecureRandom
import kotlin.time.Duration

data class OAuth2AuthorizationSession(val flow: OAuth2Flow)

interface OAuth2AuthorizationSessionStore {
    suspend fun create(flow: OAuth2Flow): String
    suspend fun consume(state: String): OAuth2AuthorizationSession?
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

    override suspend fun create(flow: OAuth2Flow): String {
        val state = ByteArray(16).also(random::nextBytes).toHexString()
        sessions.put(state, OAuth2AuthorizationSession(flow))
        return state
    }

    override suspend fun consume(state: String): OAuth2AuthorizationSession? {
        return sessions.remove(state)
    }
}
