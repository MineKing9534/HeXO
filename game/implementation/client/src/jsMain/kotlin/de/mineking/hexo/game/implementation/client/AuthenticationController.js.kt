package de.mineking.hexo.game.implementation.client

import io.ktor.client.fetchOptions
import io.ktor.client.request.HttpRequestBuilder

internal actual fun HmdApiClient.validateCookieRequestClient() {
    // Nothing to do
}

internal actual fun HttpRequestBuilder.configureCookieRequestImpl() {
    fetchOptions {
        credentials = "include"
    }
}
