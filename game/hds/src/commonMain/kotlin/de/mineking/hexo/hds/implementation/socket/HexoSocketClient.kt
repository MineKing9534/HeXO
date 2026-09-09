package de.mineking.hexo.hds.implementation.socket

import de.mineking.hexo.hds.implementation.HEXO_USER_AGENT
import de.mineking.hexo.hds.implementation.json
import de.mineking.hexo.utils.socketio.client.SocketIOClient
import de.mineking.hexo.utils.socketio.client.awaitConnect
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.http.Url
import kotlin.uuid.Uuid

internal data class AuthData(val deviceId: String, val ephemeralClientId: String, val versionHash: String = DEFAULT_VERSION_HASH) {
    fun toMap() = mapOf(
        "deviceId" to deviceId,
        "ephemeralClientId" to ephemeralClientId,
        "versionHash" to versionHash,
    )

    companion object {
        const val DEFAULT_VERSION_HASH = "HeXO-Kotlin"
    }
}

internal val logger = KotlinLogging.logger {}

data class HdsSocketOptions(
    val url: Url,
    val headers: Map<String, String?>,
    val query: Map<String, String>,
) {
    companion object {
        val DEFAULT_HEADERS = mapOf("User-Agent" to HEXO_USER_AGENT)
        val DEFAULT_QUERY_PARAMS = mapOf("skipVersionCheck" to "true")

        fun createDefault(url: String): HdsSocketOptions {
            return HdsSocketOptions(
                url = Url(url),
                headers = DEFAULT_HEADERS,
                query = DEFAULT_QUERY_PARAMS,
            )
        }
    }
}

typealias HdsSocketClient = SocketIOClient<HexoSocketEvent, HexoSocketRequest>

suspend fun connectHdsSocket(
    client: HttpClient,
    options: HdsSocketOptions,
): HdsSocketClient {
    logger.info { "Connecting HDS SocketIO..." }

    val authData = AuthData(
        deviceId = Uuid.random().toString(),
        ephemeralClientId = Uuid.random().toString(),
    )

    return SocketIOClient<HexoSocketEvent, HexoSocketRequest>(
        client = client,
        url = options.url,
        query = options.query,
        headers = options.headers,
        auth = authData.toMap(),
        format = json,
    ).awaitConnect().also {
        logger.info { "Connected" }
    }
}
