package de.mineking.hexo.utils.socketio.server

import io.ktor.server.application.plugin
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.close
import io.socket.engineio.server.EngineIoServer
import io.socket.socketio.server.SocketIoSocket
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SealedClassSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.coroutines.CoroutineContext

@PublishedApi
internal const val DEFAULT_PATH = "socket.io/"

@OptIn(InternalSerializationApi::class)
inline fun <reified Incoming : Any, Outgoing : Any> Route.socketIO(
    path: String = DEFAULT_PATH,
    noinline configure: SocketIOConfig<Incoming, Outgoing>.() -> Unit,
) = socketIO(
    incomingSerializer = application.plugin(SocketIO).format.serializersModule.serializer<Incoming>() as SealedClassSerializer<Incoming>,
    path = path,
    configure = configure,
)

@OptIn(InternalSerializationApi::class)
fun <Incoming : Any, Outgoing : Any> Route.socketIO(
    incomingSerializer: SealedClassSerializer<Incoming>,
    path: String = DEFAULT_PATH,
    configure: SocketIOConfig<Incoming, Outgoing>.() -> Unit,
) {
    val socketIO = application.plugin(SocketIO)
    val (engine, server) = socketIO.createServer()

    val config = SocketIOConfig<Incoming, Outgoing>(server.namespace("/"), socketIO.format, incomingSerializer)
    config.bind(application)
    config.configure()

    socketIOTransport(path, socketIO, engine)
}

class SocketIOSession<Incoming : Any, Outgoing : Any>(
    socket: SocketIoSocket,
    public override val incoming: ReceiveChannel<SocketIOEvent<Incoming>>,
    format: Json,
    coroutineContext: CoroutineContext,
) : Socket<Incoming, Outgoing>(socket, incoming, format, coroutineContext)

@OptIn(InternalSerializationApi::class)
inline fun <reified Incoming : Any, Outgoing : Any> Route.socketIOSession(
    path: String = DEFAULT_PATH,
    noinline handler: suspend SocketIOSession<Incoming, Outgoing>.() -> Unit,
) = socketIOSession(
    incomingSerializer = application.plugin(SocketIO).format.serializersModule.serializer<Incoming>() as SealedClassSerializer<Incoming>,
    path = path,
    handler = handler,
)

@OptIn(InternalSerializationApi::class)
fun <Incoming : Any, Outgoing : Any> Route.socketIOSession(
    incomingSerializer: SealedClassSerializer<Incoming>,
    path: String = DEFAULT_PATH,
    handler: suspend SocketIOSession<Incoming, Outgoing>.() -> Unit,
) {
    socketIO<Incoming, Outgoing>(incomingSerializer, path) {
        useIncoming = true
        onConnect {
            val session = SocketIOSession<Incoming, Outgoing>(socket, incoming, format, coroutineContext)
            handler(session)
        }
    }
}

internal fun Route.socketIOTransport(path: String, socketIO: SocketIO, engine: EngineIoServer) {
    webSocket(path) {
        if (call.request.queryParameters["transport"] != "websocket" || call.request.queryParameters["EIO"] !in setOf("3", "4")) {
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Unsupported Engine.IO transport or version"))
            return@webSocket
        }

        val connection = KtorEngineIOWebSocket(this)
        if (!socketIO.attach(connection)) {
            close(CloseReason(CloseReason.Codes.GOING_AWAY, "Server stopping"))
            return@webSocket
        }
        try {
            connection.run(engine)
        } finally {
            socketIO.detach(connection)
        }
    }
}
