package de.mineking.hexo.game.implementation.client

import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.request.HttpRequestBuilder

internal actual fun HmdApiClient.validateCookieRequestClient() {
    requireNotNull(client.httpClient.pluginOrNull(HttpCookies)) {
        "The 'HttpCookies' plugin is required for CookieAuthenticationController when using CIO"
    }
}

internal actual fun HttpRequestBuilder.configureCookieRequestImpl() {
    // Cookies are added automatically
}
