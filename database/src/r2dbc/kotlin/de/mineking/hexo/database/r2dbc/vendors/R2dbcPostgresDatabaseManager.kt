package de.mineking.hexo.database.r2dbc.vendors

import de.mineking.hexo.database.CheckConstraintViolationError
import de.mineking.hexo.database.DatabaseError
import de.mineking.hexo.database.ForeignKeyViolationError
import de.mineking.hexo.database.NotNullViolationError
import de.mineking.hexo.database.UniqueViolationError
import de.mineking.hexo.database.r2dbc.R2dbcDatabaseManager
import de.mineking.hexo.database.r2dbc.logger
import io.r2dbc.postgresql.api.ErrorDetails
import io.r2dbc.postgresql.api.PostgresqlConnection
import io.r2dbc.postgresql.api.PostgresqlException
import io.r2dbc.spi.Connection
import io.r2dbc.spi.R2dbcException
import io.r2dbc.spi.Wrapped
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.collect
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.reactivestreams.Publisher

class R2dbcPostgresDatabaseManager(
    connectionUrl: String,
) : R2dbcDatabaseManager(R2dbcDatabase.connect(connectionUrl)) {
    override val notificationFormat = Json

    private val notificationLock = Mutex()
    private val notificationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val notificationListeners = mutableMapOf<String, MutableSet<Channel<String?>>>()

    @Volatile
    private var notificationConnection: NotificationConnection? = null

    @Volatile
    private var closed = false

    override suspend fun <T> listen(type: DeserializationStrategy<T>, handler: suspend (T) -> Unit): Nothing {
        val channel = type.descriptor.serialName
        val notifications = subscribe(channel)

        try {
            for (payload in notifications) {
                @Suppress("TooGenericExceptionCaught")
                try {
                    val event = notificationFormat.decodeFromString(
                        type,
                        payload ?: notificationFormat.encodeToString(Unit),
                    )
                    handler(event)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error(e) { "Unexpected error in notification handler" }
                }
            }

            error("PostgreSQL notification stream completed")
        } finally {
            withContext(NonCancellable) {
                unsubscribe(channel, notifications)
            }
        }
    }

    private suspend fun subscribe(channel: String) = notificationLock.withLock {
        check(!closed) { "Database manager is closed" }

        val connection = notificationConnection ?: createNotificationConnection().also {
            notificationConnection = it
            collectNotifications(it)
        }

        val listeners = notificationListeners.getOrPut(channel) { mutableSetOf() }

        if (listeners.isEmpty()) {
            @Suppress("TooGenericExceptionCaught")
            try {
                connection.postgres.createStatement(listenStatement("LISTEN", channel)).execute().awaitFirst()
            } catch (error: Throwable) {
                notificationListeners.remove(channel)
                throw error
            }
        }

        Channel<String?>(Channel.BUFFERED).also { listeners += it }
    }

    private suspend fun unsubscribe(channel: String, notifications: Channel<String?>) = notificationLock.withLock {
        val listeners = notificationListeners[channel] ?: return@withLock
        listeners -= notifications

        if (listeners.isNotEmpty()) return@withLock

        notificationListeners.remove(channel)
        if (!closed) {
            @Suppress("TooGenericExceptionCaught")
            try {
                notificationConnection
                    ?.postgres
                    ?.createStatement(listenStatement("UNLISTEN", channel))
                    ?.execute()
                    ?.awaitFirst()
            } catch (error: Exception) {
                logger.error(error) { "Failed to stop listening on PostgreSQL channel $channel" }
            }
        }
    }

    private fun collectNotifications(connection: NotificationConnection) {
        notificationScope.launch(start = CoroutineStart.UNDISPATCHED) {
            var failure: Throwable? = null

            @Suppress("TooGenericExceptionCaught")
            try {
                connection.postgres.notifications.collect { notification ->
                    notificationLock.withLock {
                        notificationListeners[notification.name]?.forEach {
                            if (it.trySend(notification.parameter).isFailure) {
                                logger.warn { "Dropped PostgreSQL notification from channel ${notification.name}" }
                            }
                        }
                    }
                }
            } catch (error: Throwable) {
                failure = error
            } finally {
                withContext(NonCancellable) {
                    val listeners = notificationLock.withLock {
                        if (notificationConnection !== connection) return@withLock emptyList()

                        notificationConnection = null
                        notificationListeners.values.flatten().also { notificationListeners.clear() }
                    }
                    listeners.forEach { it.close(failure) }
                    connection.source.close().awaitFirstOrNull()
                }
            }
        }
    }

    private suspend fun createNotificationConnection(): NotificationConnection {
        val exposed = database.connector()
        @Suppress("UNCHECKED_CAST")
        val source = (exposed.connection as Publisher<Connection>).awaitFirst()
        val postgres = source.findPostgresConnection()

        if (postgres == null) {
            source.close().awaitFirstOrNull()
            error("Database connection does not wrap a PostgreSQL connection")
        }

        return NotificationConnection(source, postgres)
    }

    private data class NotificationConnection(
        val source: Connection,
        val postgres: PostgresqlConnection,
    )

    private fun Connection.findPostgresConnection(): PostgresqlConnection? {
        var current: Any? = this

        while (current != null) {
            if (current is PostgresqlConnection) return current
            if (current !is Wrapped<*>) return null

            val wrapped = runCatching { current.unwrap() }.getOrNull()
            if (wrapped === current) return null
            current = wrapped
        }

        return null
    }

    private fun listenStatement(command: String, channel: String) =
        "$command \"${channel.replace("\"", "\"\"")}\""

    private enum class PostgresSqlState(val code: String) {
        NotNullViolation("23502") {
            override fun createError(details: ErrorDetails) = NotNullViolationError(details.columnName.get())
        },
        UniqueViolation("23505") {
            override fun createError(details: ErrorDetails) = UniqueViolationError(details.constraintName.get())
        },
        ForeignKeyViolation("23503") {
            override fun createError(details: ErrorDetails) = ForeignKeyViolationError(details.constraintName.get())
        },
        CheckViolation("23514") {
            override fun createError(details: ErrorDetails) = CheckConstraintViolationError(details.constraintName.get())
        },
        ;

        abstract fun createError(details: ErrorDetails): DatabaseError
    }

    override fun extractDatabaseError(error: R2dbcException): DatabaseError {
        val cause = error.cause
        if (cause !is PostgresqlException) throw error

        val state = PostgresSqlState.entries
            .find { it.code == cause.errorDetails.code }
            ?: throw cause

        return state.createError(cause.errorDetails)
    }

    override fun close() {
        if (closed) return
        closed = true

        notificationScope.cancel()
        notificationConnection?.let { connection ->
            @Suppress("TooGenericExceptionCaught")
            try {
                runBlocking { connection.source.close().awaitFirstOrNull() }
            } catch (error: Exception) {
                logger.error(error) { "Failed to close PostgreSQL notification connection" }
            }
        }
    }
}
