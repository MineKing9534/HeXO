package de.mineking.hexo.utils.socketio.server

import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import io.socket.engineio.server.EngineIoServer
import io.socket.engineio.server.EngineIoWebSocket
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

internal class KtorEngineIOWebSocket(private val session: DefaultWebSocketServerSession) : EngineIoWebSocket() {
    private val outgoing = Channel<Frame>(Channel.BUFFERED)
    private val disposed = AtomicBoolean()

    override fun getQuery() = session.call.request.queryParameters.entries().associate { it.key to it.value.first() }
    override fun getConnectionHeaders() = session.call.request.headers.entries().associate { it.key to it.value.toList() }

    override fun write(message: String) = enqueue(Frame.Text(message))
    override fun write(message: ByteArray) = enqueue(Frame.Binary(true, message.copyOf()))

    private fun enqueue(frame: Frame) {
        if (outgoing.trySend(frame).isFailure) {
            close()
            throw IOException("Socket.IO outgoing queue is full or closed")
        }
    }

    override fun close() {
        outgoing.close()
    }

    fun dispose() {
        close()
        if (disposed.compareAndSet(false, true)) {
            emit("close")
        }
    }

    @IgnorableReturnValue
    suspend fun run(engine: EngineIoServer) = coroutineScope {
        val writer = launch {
            try {
                for (frame in outgoing) {
                    session.send(frame)
                }
            } finally {
                session.close()
            }
        }

        @Suppress("TooGenericExceptionCaught")
        try {
            engine.handleWebSocket(this@KtorEngineIOWebSocket)
            for (frame in session.incoming) {
                when (frame) {
                    is Frame.Text -> emit("message", frame.readText())
                    is Frame.Binary -> emit("message", frame.readBytes())
                    else -> Unit
                }
            }
        } catch (cause: CancellationException) {
            throw cause
        } catch (cause: Exception) {
            emit("error", "WebSocket transport error", cause.message ?: cause.toString())
        } finally {
            try {
                dispose()
            } finally {
                writer.cancel()
            }
        }
    }
}
