package de.mineking.hexo.server.api

import io.ktor.server.application.Application
import io.ktor.server.plugins.ratelimit.RateLimitConfig
import io.ktor.server.routing.Route

abstract class ApiModule {
    open fun RateLimitConfig.configureRateLimit() {}

    open fun Application.install() {}
    abstract fun Route.registerRoutes()
}
