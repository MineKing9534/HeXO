package de.mineking.hexo.game.implementation.service

import de.mineking.hexo.database.DatabaseManager
import de.mineking.hexo.game.implementation.service.profile.ProfileService
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.game.model.profile.ProfileIdentifier
import de.mineking.hexo.server.api.ApiModule
import de.mineking.hexo.utils.types.isSuccess
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail

class GameApiModule(database: DatabaseManager) : ApiModule() {
    private val profileService = ProfileService(database)

    override fun Route.registerRoutes() {
        profileRoutes()
    }

    @IgnorableReturnValue
    private fun Route.profileRoutes() = route("profiles") {
        get {
            val name = call.queryParameters.getOrFail("name")
            if (name.isBlank()) throw BadRequestException("'name' must not be blank!")

            val result = profileService.searchProfiles(name)

            call.respond(result)
        }

        fun Route.profileRoutes(identifier: (String) -> ProfileIdentifier) {
            route("{id}") {
                get {
                    val id = call.parameters.getOrFail("id")
                    val result = profileService.getProfile(identifier(id))

                    call.respond(
                        status = if (result.isSuccess()) HttpStatusCode.OK else HttpStatusCode.NotFound,
                        message = result,
                    )
                }

                get("statistics") {
                    val id = call.parameters.getOrFail("id")
                    val result = profileService.getProfileStatistics(identifier(id))

                    call.respond(
                        status = if (result.isSuccess()) HttpStatusCode.OK else HttpStatusCode.NotFound,
                        message = result,
                    )
                }
            }
        }

        profileRoutes { ProfileIdentifier.Id(ProfileId(it)) }

        route("by-name") {
            profileRoutes { ProfileIdentifier.Name(it) }
        }
    }
}
