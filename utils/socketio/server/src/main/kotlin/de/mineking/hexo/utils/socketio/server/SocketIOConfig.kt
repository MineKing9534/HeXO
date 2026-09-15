package de.mineking.hexo.utils.socketio.server

import de.mineking.hexo.utils.types.EntityId
import io.github.oshai.kotlinlogging.KotlinLogging
import io.socket.socketio.server.SocketIoNamespace
import io.socket.socketio.server.SocketIoSocket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SealedClassSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

class SocketIOEvent<out T>(@PublishedApi internal val format: Json, val data: T, val raw: Array<Any>) {
    operator fun component1() = data
    operator fun component2() = raw

    inline fun <reified Response : Any> acknowledge(noinline response: () -> Response) {
        acknowledge(format.serializersModule.serializer<Response>(), response)
    }

    fun <Response : Any> acknowledge(serializer: SerializationStrategy<Response>, response: () -> Response) {
        val callback = raw.lastOrNull() as? SocketIoSocket.ReceivedByLocalAcknowledgementCallback
            ?: return

        val payload = format.encodeToJsonElement(serializer, response())
        callback.sendAcknowledgement(JSONObject(payload.toString()))
    }
}

interface SocketIOEmitter<Outgoing : Any> {
    val format: Json

    fun send(response: Outgoing, vararg raw: Any)
}

fun EntityId.roomId() = RoomId(this)
class RoomId(id: EntityId) {
    val value = "${id::class.simpleName}.${id.value}"
}

typealias SocketIOHandler<Incoming, Outgoing, T> = suspend Socket<Incoming, Outgoing>.(SocketIOEvent<T>) -> Unit

@OptIn(InternalSerializationApi::class)
class SocketIOConfig<Incoming : Any, Outgoing : Any>(
    private val namespace: SocketIoNamespace,
    override val format: Json,
    incomingSerializer: SealedClassSerializer<Incoming>,
) : SocketIOEmitter<Outgoing> {
    internal var useIncoming = false

    private val incomingSerializers = incomingSerializer.eventSerializers(format.serializersModule)

    private val handlers = mutableListOf<SocketIOHandler<Incoming, Outgoing, Incoming>>()

    private val connectHandlers = mutableListOf<SocketIOHandler<Incoming, Outgoing, Connect>>()
    private val disconnectHandlers = mutableListOf<SocketIOHandler<Incoming, Outgoing, Disconnect>>()

    private fun send(room: RoomId?, data: Outgoing, vararg raw: Any) {
        val (name, payload) = format.encodeEvent(data)
        namespace.broadcast(room?.value, name, payload, *raw)
    }

    override fun send(response: Outgoing, vararg raw: Any) = send(null, response, raw = raw)

    fun to(room: RoomId) = object : SocketIOEmitter<Outgoing> {
        override val format = this@SocketIOConfig.format
        override fun send(response: Outgoing, vararg raw: Any) = send(room, response, raw = raw)
    }

    inline fun <reified T : Incoming> listen(noinline handler: SocketIOHandler<Incoming, Outgoing, T>) {
        listenAll {
            if (it.data !is T) return@listenAll
            handler(SocketIOEvent(format, it.data, it.raw))
        }
    }

    fun listenAll(handler: SocketIOHandler<Incoming, Outgoing, Incoming>) {
        synchronized(this@SocketIOConfig.handlers) {
            handlers += handler
        }
    }

    fun onConnect(handler: SocketIOHandler<Incoming, Outgoing, Connect>) {
        synchronized(handlers) {
            connectHandlers += handler
        }
    }

    fun onDisconnect(handler: SocketIOHandler<Incoming, Outgoing, Disconnect>) {
        synchronized(handlers) {
            disconnectHandlers += handler
        }
    }

    private suspend fun <T> Socket<Incoming, Outgoing>.dispatch(
        listeners: List<SocketIOHandler<Incoming, Outgoing, T>>,
        event: SocketIOEvent<T>,
    ) {
        for (handler in listeners) {
            @Suppress("TooGenericExceptionCaught")
            try {
                handler(event)
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: Exception) {
                logger.error(cause) { "Socket.IO handler failed for ${id.value}" }
            }
        }
    }

    inner class IncomingListener(
        private val transport: SocketIoSocket,
        private val events: Channel<SocketIOEvent<Incoming>>,
    ) : SocketIoSocket.AllEventListener {
        override fun event(eventName: String?, args: Array<Any>) {
            val serializer = incomingSerializers[eventName]
            if (serializer == null) {
                transport.send("error", "Unknown request $eventName")
                return
            }

            @Suppress("TooGenericExceptionCaught")
            val event = try {
                SocketIOEvent(format, format.decodeFromSocketIO(serializer, args), args)
            } catch (cause: Exception) {
                logger.warn(cause) { "Invalid Socket.IO event $eventName from ${transport.id}" }
                return
            }

            val accepted = events.trySend(event).isSuccess
            if (!accepted) {
                logger.warn { "Socket.IO incoming queue full or closed for ${transport.id}" }
                transport.disconnect(true)
            }
        }
    }

    internal fun bind(scope: CoroutineScope) {
        namespace.on("connection") { args ->
            val transport = args[0] as SocketIoSocket
            val events = Channel<SocketIOEvent<Incoming>>(Channel.BUFFERED)

            val job = SupervisorJob(scope.coroutineContext[Job])
            val errorHandler = CoroutineExceptionHandler { _, cause ->
                logger.error(cause) { "Socket.IO handler failed for ${transport.id}" }
            }

            val socket = Socket<Incoming, Outgoing>(transport, events, format, scope.coroutineContext + job + errorHandler)
            val disconnected = AtomicReference(SocketIOEvent(format, Disconnect("server disconnect"), emptyArray<Any>()))

            val listener = IncomingListener(transport, events)

            transport.registerAllEventListener(listener)
            transport.on("disconnect") { parameters ->
                disconnected.set(SocketIOEvent(format, Disconnect(parameters.firstOrNull()?.toString().orEmpty()), parameters))
                job.cancel()
            }

            val worker = socket.launch(start = CoroutineStart.LAZY) {
                socket.dispatch(
                    listeners = synchronized(handlers) { connectHandlers.toList() },
                    event = SocketIOEvent(format, Connect, args),
                )

                if (!useIncoming) {
                    for (event in events) {
                        socket.dispatch(synchronized(handlers) { handlers.toList() }, event)
                    }
                }
            }

            worker.invokeOnCompletion {
                transport.unregisterAllEventListener(listener)
                events.cancel()
                job.cancel()

                try {
                    transport.disconnect(true)
                } finally {
                    scope.launch(errorHandler) {
                        job.join()
                        socket.dispatch(
                            listeners = synchronized(handlers) { disconnectHandlers.toList() },
                            event = disconnected.get(),
                        )
                    }
                }
            }
            worker.start()
        }
    }
}

@JvmInline
@Serializable
value class SocketId(val value: String)

open class Socket<Incoming : Any, Outgoing : Any> internal constructor(
    internal val socket: SocketIoSocket,
    internal open val incoming: ReceiveChannel<SocketIOEvent<Incoming>>,
    override val format: Json,
    override val coroutineContext: CoroutineContext,
) : SocketIOEmitter<Outgoing>, CoroutineScope {
    val id = SocketId(socket.id)

    inline fun <reified T : Any> connectionData() = connectionData(format.serializersModule.serializer<T>())
    fun <T : Any> connectionData(serializer: DeserializationStrategy<T>) =
        format.decodeFromString(serializer, socket.connectData?.toString() ?: "{}")

    fun disconnect() {
        socket.disconnect(true)
    }

    fun joinRoom(room: String) {
        socket.joinRoom(room)
    }

    fun leaveRoom(room: String) {
        socket.leaveRoom(room)
    }

    fun leaveAllRooms() {
        socket.leaveAllRooms()
    }

    override fun send(response: Outgoing, vararg raw: Any) {
        val (name, payload) = format.encodeEvent(response)
        socket.send(name, payload, *raw)
    }
}

@PublishedApi
@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalSerializationApi::class)
internal fun <T : Any> SerializersModule.serializer(type: KClass<out T>) = serializer(type, emptyList(), false) as KSerializer<T>

internal fun Json.encodeEvent(event: Any): Pair<String, JSONObject> {
    val serializer = serializersModule.serializer(event::class)
    val payload = encodeToJsonElement(serializer, event)
    require(payload is JsonObject) { "Socket.IO events must serialize to JSON objects" }

    return serializer.descriptor.serialName to JSONObject(payload.toString())
}

internal fun <T> Json.decodeFromSocketIO(serializer: DeserializationStrategy<T>, args: Array<out Any?>): T {
    // Engine.IO appends the acknowledgement callback after the event payload.
    val parameters = args.filterNot { it is SocketIoSocket.ReceivedByLocalAcknowledgementCallback }

    val payload = when (val raw = parameters.firstOrNull()) {
        is JSONObject -> parseToJsonElement(raw.toString())
        is String -> JsonObject(mapOf("message" to JsonPrimitive(raw)))
        null -> JsonObject(emptyMap())
        else -> error("Unexpected Socket.IO event parameter type: ${raw::class}")
    }

    return decodeFromJsonElement(serializer, payload)
}

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
internal fun <T : Any> SealedClassSerializer<T>.eventSerializers(module: SerializersModule): Map<String, DeserializationStrategy<T>> {
    // Polymorphic lookup needs the module for fallback; no payload is decoded here.
    val decoder = object : AbstractDecoder() {
        override val serializersModule = module
        override fun decodeElementIndex(descriptor: SerialDescriptor): Int = error("Only serializer lookup is supported")
    }
    return descriptor.getElementDescriptor(1).elementNames.associateWith { name ->
        checkNotNull(findPolymorphicSerializerOrNull(decoder, name)) {
            "No serializer found for Socket.IO event $name"
        }
    }
}
