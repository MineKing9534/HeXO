@file:Suppress("MatchingDeclarationName")

package de.mineking.hexo.server.api

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin
import kotlinx.serialization.SerializationStrategy

class HttpResponseBody<T : Any>(val type: SerializationStrategy<T>, val body: T)
open class HttpResponseException(val status: HttpStatusCode, val body: HttpResponseBody<*>?) : RuntimeException()

fun ApplicationCall.preventCaching() {
    response.headers.append(HttpHeaders.CacheControl, "no-store")
    response.headers.append(HttpHeaders.Pragma, "no-cache")
}

fun ApplicationCall.clientAddress() = request.headers[HttpHeaders.XForwardedFor]
    ?.substringBefore(',')
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?: request.origin.remoteAddress
