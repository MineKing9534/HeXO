package de.mineking.hexo.utils.socketio.client

import com.piasy.kmp.socketio.socketio.IO
import com.piasy.kmp.socketio.socketio.Socket
import com.piasy.kmp.xlog.Logging
import com.piasy.kmp.xlog.LoggingImpl
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.http.Url
import io.ktor.http.protocolWithAuthority
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
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}
private val errorHandler = CoroutineExceptionHandler { context, cause ->
    logger.error(cause) { "Socket.IO ${context[CoroutineName]?.name ?: "operation"} failed" }
}

class ConnectErrorException(override val message: String) : Exception(message)

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
    auth: Map<String, String> = emptyMap(),
    format: Json = Json,
) = SocketIOClient<Incoming, Outgoing>(
    incomingSerializer = format.serializersModule.serializer(Incoming::class) as SealedClassSerializer<Incoming>,
    client = client,
    url = url,
    query = query,
    headers = headers,
    auth = auth,
    format = format,
)

@OptIn(InternalSerializationApi::class)
class SocketIOClient<Incoming : Any, Outgoing : Any>(
    incomingSerializer: SealedClassSerializer<Incoming>,
    private val client: HttpClient,
    private val url: Url,
    private val query: Map<String, String> = emptyMap(),
    private val headers: Map<String, String?> = emptyMap(),
    private val auth: Map<String, String> = emptyMap(),
    private val format: Json,
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

    private val requestSerializers = incomingSerializer.eventSerializers(format.serializersModule)

    private val job = SupervisorJob(client.coroutineContext[Job])
    private val scope = CoroutineScope(client.coroutineContext + job + errorHandler)

    private val outgoing = Channel<Outgoing>(Channel.BUFFERED)

    private val lock = SynchronizedObject()
    private val handlers = mutableMapOf<String, MutableList<suspend (Incoming) -> Unit>>()
    private val allHandlers = mutableListOf<suspend (Incoming) -> Unit>()

    val connected: StateFlow<Boolean>
        field = MutableStateFlow(false)

    init {
        job.invokeOnCompletion {
            synchronized(lock) { connected.value = false }
        }
    }

    suspend fun request(request: Outgoing) {
        outgoing.send(request)
    }

    fun <T : Incoming> listen(type: KClass<T>, handler: suspend (T) -> Unit): SocketListener {
        val name = format.serializersModule.serializer(type).descriptor.serialName
        check(name in requestSerializers)

        synchronized(lock) {
            val handlers = handlers.getOrPut(name) { mutableListOf() }
            handlers += {
                @Suppress("UNCHECKED_CAST")
                handler(it as T)
            }
        }

        return object : SocketListener {
            override fun remove() {
                handlers[name]?.remove(handler)
            }
        }
    }

    @IgnorableReturnValue
    inline fun <reified E : Incoming> listen(noinline handler: suspend (E) -> Unit) = listen(E::class, handler)

    @IgnorableReturnValue
    fun listenAll(handler: suspend (Incoming) -> Unit): SocketListener {
        synchronized(lock) {
            allHandlers += handler
        }

        return object : SocketListener {
            override fun remove() {
                synchronized(lock) {
                    allHandlers -= handler
                }
            }
        }
    }

    fun connect(): Deferred<SocketIOClient<Incoming, Outgoing>> = connection

    fun disconnect() {
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
            it.httpClient = client
            it.transports = listOf("websocket")
            it.path = "${url.encodedPath.trimEnd('/')}/socket.io/"
            it.query = query.toMutableMap()
            it.extraHeaders = headers.mapValues { (_, value) -> listOfNotNull(value) }
            it.auth = auth
        }) { socket ->
            if (!scope.isActive) {
                socket.close()
                return@socket
            }

            requestSerializers.forEach { (name, serializer) ->
                socket.on(name) { args ->
                    scope.launch(CoroutineName("event $name")) {
                        val parsed = when (val raw = args.singleOrNull()) {
                            is String -> {
                                @Serializable
                                data class Message(val message: String)

                                format.decodeFromJsonElement(serializer, format.encodeToJsonElement(Message(raw)))
                            }
                            is JsonObject -> format.decodeFromJsonElement(serializer, raw)
                            null -> format.decodeFromString(serializer, "{}")
                            else -> error("Unexpected event parameter type $raw")
                        }

                        dispatchListeners(name, parsed)
                    }
                }
            }

            scope.launch(CoroutineName("sender")) {
                socket.open(status)
                status.await()
                for (request in outgoing) {
                    val serializer = format.serializersModule.serializer(request::class)
                    val data = format.encodeToJsonElement(serializer, request)

                    socket.emit(serializer.descriptor.serialName, data)
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

    private suspend fun dispatchListeners(event: String, data: Incoming) = supervisorScope {
        val listeners = synchronized(lock) {
            handlers[event].orEmpty().toList() + allHandlers
        }

        listeners.forEach { handler ->
            launch(errorHandler) {
                handler(data)
            }.join()
        }
    }
}

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
