package de.mineking.hexo.sever.service

import io.ktor.server.application.Application
import io.ktor.server.routing.Route
import io.ktor.server.routing.route

abstract class WebService {
    open fun Application.setup() {}
    abstract fun Route.registerRoutes()
}

abstract class ApiWebService : WebService() {
    final override fun Route.registerRoutes() {
        route("/api") {
            registerApiRoutes()
        }
    }

    abstract fun Route.registerApiRoutes()
}
