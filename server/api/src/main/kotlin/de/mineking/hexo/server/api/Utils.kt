package de.mineking.hexo.server.api

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.origin

fun ApplicationCall.preventCaching() {
    response.headers.append(HttpHeaders.CacheControl, "no-store")
    response.headers.append(HttpHeaders.Pragma, "no-cache")
}

fun ApplicationCall.clientAddress() = request.headers[HttpHeaders.XForwardedFor]
    ?.substringBefore(',')
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?: request.origin.remoteAddress
