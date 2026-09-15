package de.mineking.hexo.hds.implementation.socket

import io.socket.client.IO
import io.socket.emitter.Emitter
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import kotlin.reflect.KClass

internal actual class SocketIOClientDriver actual constructor(
    private val json: Json,
    authData: AuthData,
    options: SocketIOOptions,
) {
    private val socket = IO.socket(
        options.host,
        IO.Options.builder()
            .setTransports(arrayOf("websocket"))
            .setExtraHeaders(options.headers.mapValues { (_, value) -> listOfNotNull(value) })
            .setPath(options.path.let { if (it.endsWith("/")) it else "$it/" })
            .setQuery(options.query.toQueryString())
            .setAuth(
                mapOf(
                    "deviceId" to authData.deviceId,
                    "ephemeralClientId" to authData.ephemeralClientId,
                    "versionHash" to authData.versionHash,
                ),
            )
            .build(),
    )

    actual fun connect() {
        socket.connect()
    }

    actual fun disconnect() {
        socket.disconnect()
    }

    @IgnorableReturnValue
    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalSerializationApi::class)
    actual fun <T : SocketEvent> listen(name: String, type: KClass<out T>, handler: (T) -> Unit): SocketListener {
        val serializer = json.serializersModule.serializer(type, emptyList(), false)
        val listener = Emitter.Listener { args ->
            @Suppress("TooGenericExceptionCaught")
            try {
                val string = when (val raw = args.firstOrNull() ?: "{}") {
                    is String -> """{"message":${json.encodeToString(raw)}}"""
                    is JSONObject -> raw.toString()
                    is Exception -> {
                        logger.error(raw) { "Socket IO $name:" }
                        """{"message":${json.encodeToString(raw.message ?: "")}}"""
                    }
                    else -> error("Unsupported event type: ${raw.javaClass}")
                }

                val event = json.decodeFromString(serializer, string) as T
                handler(event)
            } catch (e: Exception) {
                logger.error(e) { "Error while handling socket.io event of type '$name'" }
            }
        }

        socket.on(name, listener)
        return object : SocketListener {
            override fun remove() {
                socket.off(name, listener)
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    actual fun request(request: SocketRequest) {
        val serializer = json.serializersModule.serializer(request::class, emptyList(), false)
        socket.emit(request.requestName, JSONObject(json.encodeToString(serializer, request)))
    }
}

private fun Map<String, String>.toQueryString() = entries.joinToString("&") { (key, value) ->
    "${key.encodeQueryParameter()}=${value.encodeQueryParameter()}"
}

private fun String.encodeQueryParameter() = URLEncoder
    .encode(this, StandardCharsets.UTF_8)
    .replace("+", "%20")
