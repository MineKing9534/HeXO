package de.mineking.hexo.game.implementation.client

import de.mineking.hexo.game.implementation.protocol.AuthSession
import de.mineking.hexo.game.implementation.protocol.AuthSessionRefreshError
import de.mineking.hexo.game.implementation.protocol.AuthSessionRefreshRequest
import de.mineking.hexo.game.implementation.protocol.AuthSessionRevokeRequest
import de.mineking.hexo.utils.types.Result
import de.mineking.hexo.utils.types.isSuccess
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AuthenticationException : RuntimeException()

fun interface AuthenticationControllerFactory {
    fun create(client: HmdApiClient): AuthenticationController

    data object Bearer : AuthenticationControllerFactory {
        override fun create(client: HmdApiClient) = BearerAuthenticationController(client)
    }

    data object Cookie : AuthenticationControllerFactory {
        override fun create(client: HmdApiClient) = CookieAuthenticationController(client)
    }

    data object None : AuthenticationControllerFactory {
        override fun create(client: HmdApiClient) = object : AuthenticationController {
            override suspend fun HttpRequestBuilder.configure() {}
            override suspend fun refresh() = false
            override suspend fun markUpdated() {}
            override suspend fun logout() = true
        }
    }
}

interface AuthenticationController {
    suspend fun HttpRequestBuilder.configure()
    suspend fun refresh(): Boolean

    suspend fun markUpdated()
    suspend fun logout(): Boolean
}

class BearerAuthenticationController(private val client: HmdApiClient) : AuthenticationController {
    private val lock = Mutex()
    private var session: AuthSession? = null

    override suspend fun HttpRequestBuilder.configure() {
        lock.withLock {
            val session = session ?: return@withLock
            header(HttpHeaders.Authorization, session.accessToken.value)
        }
    }

    override suspend fun refresh(): Boolean {
        lock.withLock {
            val session = session ?: return false
            val response = client.doRequest("/auth/refresh") {
                method = HttpMethod.Post
                setBody(AuthSessionRefreshRequest(session.refreshToken))
            }

            if (!response.status.isSuccess()) throw AuthenticationException()
            val result = response.body<Result<AuthSession, AuthSessionRefreshError>>()
            if (!result.isSuccess()) throw AuthenticationException()

            this.session = result.value
            return true
        }
    }

    override suspend fun markUpdated() {
        client.profileRepositoryImpl.updateCurrentProfile()
    }

    override suspend fun logout(): Boolean {
        val success = lock.withLock {
            val session = session ?: return true
            val response = client.doRequest("/auth/revoke") {
                method = HttpMethod.Post
                setBody(AuthSessionRevokeRequest(session.refreshToken))
            }

            if (!response.status.isSuccess()) return false

            this.session = null
            true
        }

        client.profileRepositoryImpl.updateCurrentProfile()
        return success
    }
}

class CookieAuthenticationController(private val client: HmdApiClient) : AuthenticationController {
    private val lock = Mutex()

    init {
        client.validateCookieRequestClient()
    }

    override suspend fun HttpRequestBuilder.configure() {
        configureCookieRequestImpl()
    }

    override suspend fun refresh(): Boolean {
        lock.withLock {
            val response = client.doRequest("/auth/refresh") {
                method = HttpMethod.Post
                configureCookieRequestImpl()
            }

            return response.status.isSuccess()
        }
    }

    override suspend fun markUpdated() {
        client.profileRepositoryImpl.updateCurrentProfile()
    }

    override suspend fun logout(): Boolean {
        val success = lock.withLock {
            val response = client.doRequest("/auth/revoke") {
                method = HttpMethod.Post
                configureCookieRequestImpl()
            }

            response.status.isSuccess()
        }

        client.profileRepositoryImpl.updateCurrentProfile()
        return success
    }
}

internal expect fun HmdApiClient.validateCookieRequestClient()
internal expect fun HttpRequestBuilder.configureCookieRequestImpl()
