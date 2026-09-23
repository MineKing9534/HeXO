@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.game.implementation.service.auth

import de.mineking.hexo.game.implementation.protocol.AccessToken
import de.mineking.hexo.game.implementation.protocol.AuthSession
import de.mineking.hexo.game.implementation.service.moduleKey
import de.mineking.hexo.server.api.HttpResponseException
import de.mineking.hexo.utils.types.orThrow
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.util.date.GMTDate

internal const val ACCESS_COOKIE = "__Host-hexo_access"
internal const val REFRESH_COOKIE = "__Secure-hexo_refresh"

class AuthenticationFailedException(status: HttpStatusCode = HttpStatusCode.Unauthorized) : HttpResponseException(status, null)

internal fun <T : Any> either(first: T?, second: T?) = when {
    first == null && second == null -> null
    first != null && second != null -> throw AuthenticationFailedException()
    first != null -> first
    second != null -> second
    else -> error("")
}

private fun ApplicationCall.accessToken(): AccessToken? {
    val header = request.headers[HttpHeaders.Authorization]
    val cookie = request.cookies[ACCESS_COOKIE]

    return either(header, cookie)?.let { AccessToken(it) }
}

private suspend fun ApplicationCall.user0(): User {
    val manager = attributes[moduleKey].sessionManager
    val token = accessToken() ?: return User.Anonymous

    val profile = manager.validateSession(token)
        .orThrow { AuthenticationFailedException() }

    return User.Authenticated(profile)
}

internal suspend inline fun <reified T : User> ApplicationCall.user() = user0() as? T
    ?: throw AuthenticationFailedException(HttpStatusCode.Forbidden)

// TODO correct base path

internal fun ApplicationCall.setSessionCookies(sessionManager: AuthSessionManager, session: AuthSession) {
    response.cookies.append(
        Cookie(
            name = ACCESS_COOKIE,
            value = session.accessToken.value,
            path = "/",
            secure = true,
            httpOnly = true,
            maxAge = sessionManager.accessTokenTtl.inWholeSeconds.toInt(),
            extensions = mapOf("SameSite" to "Strict"),
        ),
    )

    response.cookies.append(
        Cookie(
            name = REFRESH_COOKIE,
            value = session.refreshToken.value,
            path = "/auth",
            secure = true,
            httpOnly = true,
            maxAge = sessionManager.refreshTokenTtl.inWholeSeconds.toInt(),
            extensions = mapOf("SameSite" to "Strict"),
        ),
    )
}

internal fun ApplicationCall.clearSessionCookies() {
    response.cookies.append(
        Cookie(
            name = ACCESS_COOKIE,
            value = "",
            path = "/",
            secure = true,
            httpOnly = true,
            maxAge = 0,
            expires = GMTDate.START,
            extensions = mapOf("SameSite" to "Strict"),
        ),
    )
    response.cookies.append(
        Cookie(
            name = REFRESH_COOKIE,
            value = "",
            path = "/auth",
            secure = true,
            httpOnly = true,
            maxAge = 0,
            expires = GMTDate.START,
            extensions = mapOf("SameSite" to "Strict"),
        ),
    )
}
