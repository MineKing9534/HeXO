package de.mineking.hexo.server

import de.mineking.hexo.server.api.ApiModule
import de.mineking.hexo.utils.socketio.server.SocketIO
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.socket.engineio.server.EngineIoServerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger {}

class HttpServer(modules: List<ApiModule>, port: Int) {
    private val json = Json {
        allowStructuredMapKeys = true
        encodeDefaults = false
    }

    private val server = embeddedServer(Netty, port = port) {
        install(ContentNegotiation) {
            json(json)
        }

        install(IgnoreTrailingSlash)
        install(CORS) {
            anyHost()
            allowMethod(HttpMethod.Post)
            allowHeader(HttpHeaders.ContentType)
        }

        install(WebSockets)
        install(SocketIO) {
            format = json
            engineOptions.allowedCorsOrigins = EngineIoServerOptions.ALLOWED_CORS_ORIGIN_ALL
        }

        install(RateLimit) {
            modules.forEach {
                it.run {
                    configureRateLimit()
                }
            }
        }

        installErrorHandling()

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

private fun Application.installErrorHandling() {
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
    }

    routing {
        route("{path...}") {
            handle {
                call.respondError(
                    status = HttpStatusCode.NotFound,
                    message = "There is no API endpoint at this address. Check the URL and try again.",
                )
            }
        }
    }
}

private suspend fun ApplicationCall.respondError(status: HttpStatusCode, message: String) {
    respond(status, ErrorResponse(status.value, status.description, message))
}

@Serializable
private data class ErrorResponse(val status: Int, val error: String, val message: String)
