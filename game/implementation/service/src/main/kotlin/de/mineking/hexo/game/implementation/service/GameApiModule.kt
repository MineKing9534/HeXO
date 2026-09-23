package de.mineking.hexo.game.implementation.service

import de.mineking.hexo.database.DatabaseManager
import de.mineking.hexo.game.implementation.protocol.AccessToken
import de.mineking.hexo.game.implementation.protocol.AuthSessionRefreshRequest
import de.mineking.hexo.game.implementation.protocol.AuthSessionRevokeRequest
import de.mineking.hexo.game.implementation.protocol.RefreshToken
import de.mineking.hexo.game.implementation.service.auth.ACCESS_COOKIE
import de.mineking.hexo.game.implementation.service.auth.AuthSessionManager
import de.mineking.hexo.game.implementation.service.auth.AuthenticationFailedException
import de.mineking.hexo.game.implementation.service.auth.REFRESH_COOKIE
import de.mineking.hexo.game.implementation.service.auth.User
import de.mineking.hexo.game.implementation.service.auth.clearSessionCookies
import de.mineking.hexo.game.implementation.service.auth.either
import de.mineking.hexo.game.implementation.service.auth.setSessionCookies
import de.mineking.hexo.game.implementation.service.auth.user
import de.mineking.hexo.game.implementation.service.profile.ProfileService
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.game.model.profile.ProfileIdentifier
import de.mineking.hexo.server.api.ApiModule
import de.mineking.hexo.server.api.preventCaching
import de.mineking.hexo.utils.types.isSuccess
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondNullable
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail
import io.ktor.util.AttributeKey

internal val moduleKey = AttributeKey<GameApiModule>("module")

class GameApiModule(
    database: DatabaseManager,
    internal val sessionManager: AuthSessionManager,
) : ApiModule() {
    private val profileService = ProfileService(database)

    override fun Route.registerRoutes() {
        install(createRouteScopedPlugin("GameApiModulePlugin") {
            onCall {
                it.attributes[moduleKey] = this@GameApiModule
            }
        })

        authRoutes()
        profileRoutes()
    }

    @IgnorableReturnValue
    private fun Route.authRoutes() = route("auth") {
        post("refresh") {
            call.preventCaching()

            val request = runCatching { call.receive<AuthSessionRefreshRequest>() }.getOrNull()
            val cookie = call.request.cookies[REFRESH_COOKIE]

            val token = either(request?.refreshToken, cookie?.let { RefreshToken(it) })
                ?: throw AuthenticationFailedException()

            val result = sessionManager.refreshSession(token)
            if (cookie != null && result.isSuccess()) call.setSessionCookies(sessionManager, result.value)

            call.respondNullable(
                status = if (result.isSuccess()) HttpStatusCode.OK else HttpStatusCode.Unauthorized,
                message = result.takeIf { request != null || !result.isSuccess() },
            )
        }

        post("revoke") {
            call.preventCaching()

            val request = runCatching { call.receive<AuthSessionRevokeRequest>() }.getOrNull()
            val cookie = call.request.cookies[REFRESH_COOKIE]
                ?.let { RefreshToken(it) }
                ?: call.request.cookies[ACCESS_COOKIE]
                    ?.let { AccessToken(it) }

            val token = either(request?.token, cookie)
                ?: throw AuthenticationFailedException()

            val result = sessionManager.revokeSession(token)
            if (cookie != null) call.clearSessionCookies()

            call.respond(
                status = if (result.isSuccess()) HttpStatusCode.OK else HttpStatusCode.Unauthorized,
                message = result,
            )
        }
    }

    @IgnorableReturnValue
    private fun Route.profileRoutes() = route("profiles") {
        get {
            val name = call.queryParameters.getOrFail("name")
            if (name.isBlank()) throw BadRequestException("'name' must not be blank!")

            val result = profileService.searchProfiles(name)

            call.respond(result)
        }

        fun Route.profileRoutes(identifier: suspend ApplicationCall.(String) -> ProfileIdentifier) {
            route("{id}") {
                get {
                    val id = call.parameters.getOrFail("id")
                    val result = profileService.getProfile(call.identifier(id))

                    call.respond(
                        status = if (result.isSuccess()) HttpStatusCode.OK else HttpStatusCode.NotFound,
                        message = result,
                    )
                }

                get("statistics") {
                    val id = call.parameters.getOrFail("id")
                    val result = profileService.getProfileStatistics(call.identifier(id))

                    call.respond(
                        status = if (result.isSuccess()) HttpStatusCode.OK else HttpStatusCode.NotFound,
                        message = result,
                    )
                }
            }
        }

        profileRoutes {
            if (it == "@me") {
                val user = user<User.Authenticated>()
                return@profileRoutes user.profileId
            }

            ProfileId(it)
        }

        route("by-name") {
            profileRoutes { ProfileIdentifier.Name(it) }
        }
    }
}
