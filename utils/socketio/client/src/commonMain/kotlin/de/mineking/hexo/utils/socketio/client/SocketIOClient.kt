package de.mineking.hexo.utils.socketio.client

import com.piasy.kmp.socketio.socketio.Ack
import com.piasy.kmp.socketio.socketio.IO
import com.piasy.kmp.socketio.socketio.Socket
import com.piasy.kmp.xlog.Logging
import com.piasy.kmp.xlog.LoggingImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.http.Url
import io.ktor.http.protocolWithAuthority
import io.ktor.utils.io.CancellationException
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SealedClassSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}
private val errorHandler = CoroutineExceptionHandler { context, cause ->
    logger.error(cause) { "Socket.IO ${context[CoroutineName]?.name ?: "operation"} failed" }
}

class ConnectErrorException(override val message: String) : Exception(message)

class SocketIOEvent<out T>(val data: T, val raw: Array<out Any>) {
    operator fun component1() = data
    operator fun component2() = raw
}

interface SocketListener {
    fun remove()
}

@Suppress("FunctionNaming")
@OptIn(InternalSerializationApi::class)
inline fun <reified Incoming : Any, reified Outgoing : Any> SocketIOClient(
    client: HttpClient,
    url: Url,
    query: Map<String, String> = emptyMap(),
    headers: Map<String, String?> = emptyMap(),
    connectionData: Any = Unit,
    format: Json = Json,
) = SocketIOClient<Incoming, Outgoing>(
    incomingSerializer = format.serializersModule.serializer(Incoming::class) as SealedClassSerializer<Incoming>,
    client = client,
    url = url,
    query = query,
    headers = headers,
    connectionData = connectionData,
    format = format,
)

@OptIn(InternalSerializationApi::class)
class SocketIOClient<Incoming : Any, Outgoing : Any>(
    incomingSerializer: SealedClassSerializer<Incoming>,
    private val client: HttpClient,
    private val url: Url,
    private val query: Map<String, String> = emptyMap(),
    private val headers: Map<String, String?> = emptyMap(),
    private val connectionData: Any = Unit,
    val format: Json,
) {
    companion object {
        init {
            @Suppress("EmptyFunctionBlock")
            Logging.init(object : LoggingImpl {
                override fun debug() = false

                override fun debug(tag: String, content: String) {}
                override fun info(tag: String, content: String) {}
                override fun error(tag: String, content: String) = logger.error { "$tag $content" }
            })
        }
    }

    private val incomingSerializers = incomingSerializer.eventSerializers(format.serializersModule)

    private val job = SupervisorJob(client.coroutineContext[Job])
    private val scope = CoroutineScope(client.coroutineContext + job + errorHandler)

    private val outgoing = Channel<Pair<SocketIOEvent<Outgoing>, Ack?>>(Channel.BUFFERED)

    private val lock = SynchronizedObject()
    private val pendingAcks = mutableSetOf<CompletableDeferred<*>>()

    private fun failAcknowledgements() {
        val pending = synchronized(lock) {
            pendingAcks.toList()
                .also { pendingAcks.clear() }
        }

        pending.forEach {
            it.completeExceptionally(IllegalStateException("Socket.IO disconnected"))
        }
    }

    private val handlers = mutableListOf<suspend (SocketIOEvent<Incoming>) -> Unit>()

    val connected: StateFlow<Boolean>
        field = MutableStateFlow(false)

    init {
        job.invokeOnCompletion {
            synchronized(lock) { connected.value = false }
        }
    }

    @IgnorableReturnValue
    fun request(request: Outgoing, vararg raw: Any) = outgoing.trySend(SocketIOEvent(request, raw) to null).isSuccess

    suspend fun <A : Any> requestAwait(request: Outgoing, vararg raw: Any, ack: DeserializationStrategy<A>): A {
        val result = CompletableDeferred<A>()
        synchronized(lock) {
            check(connected.value) { "Socket.IO is not connected" }
            pendingAcks += result
        }
        try {
            outgoing.send(SocketIOEvent(request, raw) to CoroutineAck(result, format, ack))
            return result.await()
        } finally {
            synchronized(lock) { pendingAcks -= result }
            result.cancel()
        }
    }

    suspend inline fun <reified A : Any> requestAwait(request: Outgoing, vararg raw: Any) =
        requestAwait(request, raw = raw, ack = format.serializersModule.serializer<A>())

    @IgnorableReturnValue
    inline fun <reified T : Incoming> listen(noinline handler: suspend (SocketIOEvent<T>) -> Unit) = listenAll {
        if (it.data !is T) return@listenAll
        handler(SocketIOEvent(it.data, it.raw))
    }

    @IgnorableReturnValue
    fun listenAll(handler: suspend (SocketIOEvent<Incoming>) -> Unit): SocketListener {
        synchronized(lock) {
            handlers += handler
        }

        return object : SocketListener {
            override fun remove() {
                synchronized(lock) {
                    handlers -= handler
                }
            }
        }
    }

    fun connect(): Deferred<SocketIOClient<Incoming, Outgoing>> = connection

    fun disconnect() {
        failAcknowledgements()
        scope.cancel()
        synchronized(lock) { connected.value = false }
        outgoing.cancel()
    }

    private val connection: Deferred<SocketIOClient<Incoming, Outgoing>> by lazy {
        val status = CompletableDeferred<SocketIOClient<Incoming, Outgoing>>(job)
        status.invokeOnCompletion { cause ->
            if (cause != null) disconnect()
        }
        job.invokeOnCompletion { outgoing.cancel() }

        IO.socket(url.protocolWithAuthority, IO.Options().also {
            it.forceNew = true
            it.httpClient = client
            it.transports = listOf("websocket")
            it.path = "${url.encodedPath.trimEnd('/')}/socket.io/"
            it.query = query.toMutableMap()
            it.extraHeaders = headers.mapValues { (_, value) -> listOfNotNull(value) }
            it.auth = format.encodeToJsonElement(
                serializer = format.serializersModule.serializer(connectionData::class),
                value = connectionData,
            ).jsonObject.mapValues { (_, value) -> value.jsonPrimitive.content }
        }) { socket ->
            if (!scope.isActive) {
                socket.close()
                return@socket
            }

            incomingSerializers.forEach { (name, serializer) ->
                socket.on(name) { args ->
                    scope.launch(CoroutineName("event $name")) {
                        val parsed = format.decodeFromSocketIO(serializer, args)

                        dispatchListeners(SocketIOEvent(parsed, args))
                    }
                }
            }

            scope.launch(CoroutineName("sender")) {
                socket.open(status)
                status.await()
                for ((request, ack) in outgoing) {
                    if (ack is CoroutineAck<*> && !ack.isActive) continue
                    if (ack != null && !connected.value) {
                        failAcknowledgements()
                        continue
                    }

                    val serializer = format.serializersModule.serializer(request.data::class)
                    val encoded = format.encodeToJsonElement(serializer, request.data)

                    @Suppress("SpreadOperator")
                    socket.emit(serializer.descriptor.serialName, encoded, *request.raw, *if (ack == null) emptyArray() else arrayOf(ack))
                }
            }.invokeOnCompletion { cause ->
                if (cause != null) {
                    status.completeExceptionally(cause)
                    disconnect()
                }
                socket.close()
            }
        }

        status
    }

    private fun Socket.open(status: CompletableDeferred<SocketIOClient<Incoming, Outgoing>>) {
        // Socket.IO emits connect for both the initial connection and reconnections.
        on(Socket.EVENT_CONNECT) {
            synchronized(lock) {
                this@SocketIOClient.connected.value = job.isActive
            }
            status.complete(this@SocketIOClient)
        }

        on(Socket.EVENT_DISCONNECT) {
            failAcknowledgements()
            synchronized(lock) {
                this@SocketIOClient.connected.value = false
            }
        }

        on(Socket.EVENT_CONNECT_ERROR) { args ->
            synchronized(lock) {
                this@SocketIOClient.connected.value = false
            }

            val message = when (val arg = args.singleOrNull()) {
                is Throwable -> arg.message.orEmpty()
                else -> arg?.toString().orEmpty()
            }
            status.completeExceptionally(ConnectErrorException(message))
        }

        @Suppress("TooGenericExceptionCaught")
        try {
            open()
        } catch (e: Exception) {
            status.completeExceptionally(e)
        }
    }

    private suspend fun dispatchListeners(data: SocketIOEvent<Incoming>) = supervisorScope {
        val listeners = synchronized(lock) { handlers.toList() }
        for (handler in listeners) {
            @Suppress("TooGenericExceptionCaught")
            try {
                handler(data)
            } catch (cause: CancellationException) {
                throw cause
            } catch (cause: Exception) {
                logger.error(cause) { "Socket.IO handler failed" }
            }
        }
    }
}

@IgnorableReturnValue
suspend fun <Incoming : Any, Outgoing : Any> SocketIOClient<Incoming, Outgoing>.awaitConnect() = connect().await()

@PublishedApi
@Suppress("UNCHECKED_CAST")
@OptIn(ExperimentalSerializationApi::class)
internal fun <T : Any> SerializersModule.serializer(type: KClass<out T>) = serializer(type, emptyList(), false) as KSerializer<T>

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

private fun <T> Json.decodeFromSocketIO(serializer: DeserializationStrategy<T>, args: Array<out Any>) = when (val raw = args.firstOrNull()) {
    is String -> {
        @Serializable
        data class Message(val message: String)

        decodeFromJsonElement(serializer, encodeToJsonElement(Message(raw)))
    }
    is JsonObject -> decodeFromJsonElement(serializer, raw)
    null -> decodeFromString(serializer, "{}")
    else -> error("Unexpected event parameter type $raw")
}

private class CoroutineAck<T>(
    private val result: CompletableDeferred<T>,
    private val format: Json,
    private val serializer: DeserializationStrategy<T>,
) : Ack {
    val isActive get() = result.isActive

    override fun call(vararg args: Any) {
        if (!result.isActive) return
        runCatching { format.decodeFromSocketIO(serializer, args) }
            .onSuccess { result.complete(it) }
            .onFailure { result.completeExceptionally(it) }
    }
}
