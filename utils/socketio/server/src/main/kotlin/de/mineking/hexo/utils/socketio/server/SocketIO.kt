package de.mineking.hexo.utils.socketio.server

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopPreparing
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.server.application.plugin
import io.ktor.server.websocket.WebSockets
import io.ktor.util.AttributeKey
import io.ktor.utils.io.KtorDsl
import io.socket.engineio.server.EngineIoServer
import io.socket.engineio.server.EngineIoServerOptions
import io.socket.socketio.server.SocketIoServer
import io.socket.socketio.server.SocketIoServerOptions
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger {}

class SocketIO private constructor(private val options: SocketIOOptions) {
    val format: Json = options.format

    @KtorDsl
    class SocketIOOptions {
        var format: Json = Json
        val engineOptions: EngineIoServerOptions = EngineIoServerOptions.newFromDefault()
        val serverOptions: SocketIoServerOptions = SocketIoServerOptions.newFromDefault()
    }

    private val connections = mutableSetOf<KtorEngineIOWebSocket>()
    private val engines = mutableSetOf<EngineIoServer>()
    private var stopped = false

    internal fun createServer(): Pair<EngineIoServer, SocketIoServer> = synchronized(connections) {
        check(!stopped) { "Socket.IO server is stopping" }
        val engine = EngineIoServer(options.engineOptions)
        @Suppress("TooGenericExceptionCaught")
        try {
            val server = SocketIoServer(engine, options.serverOptions)
            engines += engine
            engine to server
        } catch (cause: Throwable) {
            engine.shutdown()
            throw cause
        }
    }

    internal fun attach(connection: KtorEngineIOWebSocket): Boolean = synchronized(connections) {
        if (stopped) return@synchronized false
        connections += connection
        true
    }

    internal fun detach(connection: KtorEngineIOWebSocket) {
        synchronized(connections) {
            connections -= connection
        }
    }

    private fun shutdown() {
        val (active, servers) = synchronized(connections) {
            stopped = true
            (connections.toList() to engines.toList()).also {
                connections.clear()
                engines.clear()
            }
        }
        try {
            active.forEach { connection ->
                runCatching { connection.dispose() }.onFailure {
                    logger.warn(it) { "Socket.IO disconnect listener failed" }
                }
            }
        } finally {
            servers.forEach { it.shutdown() }
        }
    }

    companion object Plugin : BaseApplicationPlugin<Application, SocketIOOptions, SocketIO> {
        override val key: AttributeKey<SocketIO> = AttributeKey("SocketIO")

        override fun install(pipeline: Application, configure: SocketIOOptions.() -> Unit): SocketIO {
            pipeline.plugin(WebSockets)
            val socketIO = SocketIO(SocketIOOptions().apply(configure))

            pipeline.monitor.subscribe(ApplicationStopPreparing) {
                logger.trace { "Shutdown SocketIO due to application stop" }
                socketIO.shutdown()
            }

            return socketIO
        }
    }
}
