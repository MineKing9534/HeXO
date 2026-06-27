package de.mineking.hexo.server

import de.mineking.hexo.server.html.CardColorScheme
import de.mineking.hexo.server.html.statusPage
import de.mineking.hexo.sever.service.WebService
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.html.respondHtml
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class HttpServer(services: List<WebService>, port: Int) {
    private val logger = KotlinLogging.logger {}

    private val server = embeddedServer(Netty, port = port) {
        install(ContentNegotiation) {
            json(Json)
        }

        install(StatusPages) {
            exception<Exception> { call, cause ->
                if (cause !is BadRequestException) {
                    logger.error(cause) { "Unexpected error in http handler" }
                }

                call.respondError(
                    status = HttpStatusCode.InternalServerError,
                    message = "The server could not complete this request. Please try again in a moment.",
                )
            }

            status(HttpStatusCode.NotFound) {
                call.respondError(
                    status = HttpStatusCode.NotFound,
                    message = "There is no HeXO page or service at this address. Check the link and try again.",
                )
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

private suspend fun ApplicationCall.respondError(status: HttpStatusCode, message: String) {
    when (acceptedErrorResponseFormat()) {
        ErrorResponseFormat.Json -> respond(status, ErrorResponse(status.value, status.description, message))

        ErrorResponseFormat.Html -> respondHtml(status) {
            statusPage(CardColorScheme.Error, title = "${status.value} ${status.description}") {
                +message
            }
        }

        ErrorResponseFormat.Text -> respondText(
            text = "${status.value} ${status.description}: $message",
            contentType = ContentType.Text.Plain,
            status = status,
        )
    }
}

private fun ApplicationCall.acceptedErrorResponseFormat(): ErrorResponseFormat {
    return request.header(HttpHeaders.Accept)
        ?.split(",")
        ?.mapNotNull { it.toAcceptedMediaType() }
        ?.firstNotNullOfOrNull { it.toErrorResponseFormat() }
        ?: ErrorResponseFormat.Text
}

private fun String.toAcceptedMediaType(): String? {
    val parts = split(';').map { it.trim() }
    val quality = parts
        .drop(1)
        .firstNotNullOfOrNull {
            val (name, value) = it.split('=', limit = 2).let { parameter ->
                parameter.first() to parameter.getOrNull(1)
            }

            value?.takeIf { name.equals("q", ignoreCase = true) }?.toDoubleOrNull()
        } ?: 1.0

    return parts.first().lowercase().takeIf { quality > 0.0 }
}

private fun String.toErrorResponseFormat(): ErrorResponseFormat? {
    return when (this) {
        ContentType.Application.Json.toString().lowercase(), "${ContentType.Application.Json.contentType}/*", "*/*" -> ErrorResponseFormat.Json
        ContentType.Text.Html.toString().lowercase(), "${ContentType.Text.Html.contentType}/*" -> ErrorResponseFormat.Html
        ContentType.Text.Plain.toString().lowercase() -> ErrorResponseFormat.Text
        else -> null
    }
}

private enum class ErrorResponseFormat {
    Json,
    Html,
    Text,
}

@Serializable
private data class ErrorResponse(val status: Int, val error: String, val message: String)
