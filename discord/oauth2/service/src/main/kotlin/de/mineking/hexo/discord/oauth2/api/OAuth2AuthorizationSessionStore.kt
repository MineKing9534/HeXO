package de.mineking.hexo.discord.oauth2.api

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.expireAfterWrite
import de.mineking.hexo.discord.oauth2.model.OAuth2Flow
import java.security.SecureRandom
import kotlin.time.Duration.Companion.minutes

data class OAuth2AuthorizationSession(val flow: OAuth2Flow)

interface OAuth2AuthorizationSessionStore {
    suspend fun create(flow: OAuth2Flow): String
    suspend fun consume(state: String): OAuth2AuthorizationSession?
}

class InMemoryOAuth2AuthorizationSessionStore : OAuth2AuthorizationSessionStore {
    private val random = SecureRandom()
    private val sessions = Caffeine.newBuilder()
        .expireAfterWrite(5.minutes)
        .build<String, OAuth2AuthorizationSession>()

    override suspend fun create(flow: OAuth2Flow): String {
        val state = ByteArray(16).also(random::nextBytes).toHexString()
        sessions.put(state, OAuth2AuthorizationSession(flow))
        return state
    }

    override suspend fun consume(state: String): OAuth2AuthorizationSession? {
        return sessions.asMap().remove(state)
    }
}
