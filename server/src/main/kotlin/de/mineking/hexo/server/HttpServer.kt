package de.mineking.hexo.server

import de.mineking.hexo.server.html.CardColorScheme
import de.mineking.hexo.server.html.statusPage
import de.mineking.hexo.sever.service.WebService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.html.respondHtml
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.routing.routing

class HttpServer(services: List<WebService>, port: Int) {
    private val logger = KotlinLogging.logger {}

    private val server = embeddedServer(Netty, port = port) {
        install(StatusPages) {
            exception<Exception> { call, cause ->
                if (cause !is BadRequestException) {
                    logger.error(cause) { "Unexpected error in http handler" }
                }

                call.respondHtml {
                    statusPage(CardColorScheme.Error, title = "500 Internal Server Error") {
                        +"The server could not complete this request. Please try again in a moment."
                    }
                }
            }

            status(HttpStatusCode.NotFound) {
                call.respondHtml {
                    statusPage(CardColorScheme.Error, title = "404 Not Found") {
                        +"There is no HeXO page or service at this address. Check the link and try again."
                    }
                }
            }
        }

        services.forEach {
            it.run {
                setup()
            }
        }

        routing {
            staticResources("/static", "static", index = null)
            services.forEach {
                it.run {
                    registerRoutes()
                }
            }
        }
    }

    fun start(wait: Boolean) {
        server.start(wait = wait)
    }

    fun stop() {
        server.stop()
    }
}
