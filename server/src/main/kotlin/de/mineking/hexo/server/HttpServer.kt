package de.mineking.hexo.server

import de.mineking.hexo.server.api.ApiModule
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class HttpServer(modules: List<ApiModule>, port: Int) {
    private val logger = KotlinLogging.logger {}

    private val server = embeddedServer(Netty, port = port) {
        install(ContentNegotiation) {
            json(Json)
        }

        install(StatusPages) {
            exception<BadRequestException> { call, cause ->
                logger.debug(cause) { "Rejected invalid HTTP request" }
                call.respondError(
                    status = HttpStatusCode.BadRequest,
                    message = "The request is invalid. Check its parameters and body and try again.",
                )
            }

            exception<Exception> { call, cause ->
                if (cause is CancellationException) throw cause

                logger.error(cause) { "Unexpected error in HTTP handler" }

                if (!call.response.isCommitted) {
                    call.respondError(
                        status = HttpStatusCode.InternalServerError,
                        message = "The server could not complete this request. Please try again in a moment.",
                    )
                }
            }

            status(HttpStatusCode.NotFound) {
                call.respondError(
                    status = HttpStatusCode.NotFound,
                    message = "There is no API endpoint at this address. Check the URL and try again.",
                )
            }
        }

        modules.forEach {
            it.run {
                install()
            }
        }

        routing {
            modules.forEach {
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

private suspend fun ApplicationCall.respondError(status: HttpStatusCode, message: String) {
    respond(status, ErrorResponse(status.value, status.description, message))
}

@Serializable
private data class ErrorResponse(val status: Int, val error: String, val message: String)
