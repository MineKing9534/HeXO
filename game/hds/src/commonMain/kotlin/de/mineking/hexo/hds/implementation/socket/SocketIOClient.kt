package de.mineking.hexo.hds.implementation.socket

import de.mineking.hexo.hds.implementation.HEXO_USER_AGENT
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.Url
import io.ktor.http.protocolWithAuthority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.reflect.KClass
import kotlin.uuid.Uuid

internal data class AuthData(val deviceId: String, val ephemeralClientId: String, val versionHash: String = DEFAULT_VERSION_HASH) {
    companion object {
        const val DEFAULT_VERSION_HASH = "HeXO-Kotlin"
    }
}

internal val logger = KotlinLogging.logger {}

data class SocketIOOptions(
    val host: String,
    val path: String,
    val headers: Map<String, String?>,
    val query: Map<String, String>,
) {
    companion object {
        val DEFAULT_HEADERS = mapOf("User-Agent" to HEXO_USER_AGENT)
        val DEFAULT_QUERY_PARAMS = mapOf("skipVersionCheck" to "true")

        fun createDefault(url: String): SocketIOOptions {
            val url = Url(url)
            return SocketIOOptions(
                host = url.protocolWithAuthority,
                path = "${url.encodedPath.trimEnd('/')}/socket.io",
                headers = DEFAULT_HEADERS,
                query = DEFAULT_QUERY_PARAMS,
            )
        }
    }
}

suspend fun connectSocketClient(json: Json = de.mineking.hexo.hds.implementation.json, options: SocketIOOptions): SocketIOClient {
    logger.info { "Connecting..." }

    val authData = AuthData(
        deviceId = Uuid.random().toString(),
        ephemeralClientId = Uuid.random().toString(),
    )

    return SocketIOClient(SocketIOClientDriver(json, authData, options))
        .awaitConnect()
        .also { logger.info { "Connected" } }
}

class SocketIOClient internal constructor(
    private val driver: SocketIOClientDriver,
) {
    @IgnorableReturnValue
    fun <T : SocketEvent> listen(type: KClass<T>, handler: SocketListener.(T) -> Unit): SocketListener {
        lateinit var listener: SocketListener
        return driver.listen(type.eventName, type) {
            listener.handler(it)
        }.also {
            listener = it
        }
    }

    fun request(request: SocketRequest) {
        driver.request(request)
    }

    @IgnorableReturnValue
    suspend fun awaitConnect() = suspendCancellableCoroutine { continuation ->
        var connectListener: SocketListener? = null
        var connectErrorListener: SocketListener? = null

        fun removeListeners() {
            connectListener?.remove()
            connectErrorListener?.remove()
            connectListener = null
            connectErrorListener = null
        }

        connectListener = listen<ProtocolSocketEvent.Connected> {
            removeListeners()
            continuation.resume(this@SocketIOClient)
        }

        connectErrorListener = listen<ProtocolSocketEvent.ConnectError> { event ->
            removeListeners()
            continuation.resumeWithException(ConnectErrorException(event.message))
        }

        continuation.invokeOnCancellation {
            removeListeners()
        }

        @Suppress("TooGenericExceptionCaught")
        try {
            driver.connect()
        } catch (e: Exception) {
            removeListeners()
            continuation.resumeWithException(e)
        }
    }

    fun disconnect() {
        driver.disconnect()
    }
}

@IgnorableReturnValue
inline fun <reified T : SocketEvent> SocketIOClient.listen(noinline handler: SocketListener.(T) -> Unit) =
    listen(T::class, handler)

interface SocketListener {
    fun remove()
}

internal expect class SocketIOClientDriver(
    json: Json,
    authData: AuthData,
    options: SocketIOOptions,
) {
    fun connect()
    fun disconnect()

    @IgnorableReturnValue
    fun <T : SocketEvent> listen(name: String, type: KClass<out T>, handler: (T) -> Unit): SocketListener

    fun request(request: SocketRequest)
}

private class ConnectErrorException(override val message: String) : Exception(message)
