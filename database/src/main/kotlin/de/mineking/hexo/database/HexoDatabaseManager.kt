package de.mineking.hexo.database

import de.mineking.hexo.utils.types.Result
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.core.SqlLogger
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.statements.StatementContext
import org.jetbrains.exposed.v1.core.statements.expandArgs
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.postgresql.util.PSQLException
import org.postgresql.util.ServerErrorMessage
import java.sql.SQLException
import java.util.ServiceLoader

internal val logger = KotlinLogging.logger {}

class HexoDatabaseManager(url: String) {
    private val database = Database.connect(url)

    init {
        runBlocking {
            transaction(readOnly = false) {
                try {
                    ServiceLoader.load(Migration::class.java).forEach {
                        logger.info { "Running database migration ${it::class.qualifiedName}" }

                        it.run {
                            migrate()
                        }
                    }
                } catch (e: SQLException) {
                    throw UnexpectedDatabaseErrorException.Unknown(e)
                }
            }
        }
    }

    suspend fun <T> transaction(
        readOnly: Boolean,
        block: JdbcTransaction.() -> T,
    ): Result<T, DatabaseError> {
        return try {
            suspendTransaction(database, readOnly = readOnly) {
                if (logger.isDebugEnabled()) {
                    addLogger(object : SqlLogger {
                        override fun log(context: StatementContext, transaction: Transaction) {
                            logger.debug { context.expandArgs(this@suspendTransaction) }
                        }
                    })
                }

                try {
                    Result.Success(block())
                } catch (e: SQLException) {
                    throw UnexpectedDatabaseErrorException.Known(extractDatabaseError(e))
                }
            }
        } catch (e: UnexpectedDatabaseErrorException.Known) {
            Result.Error(e.error)
        }
    }

    private enum class PostgresSqlState(val code: String) {
        NotNullViolation("23502") {
            override fun createError(details: ServerErrorMessage) = NotNullViolationError(details.column!!)
        },
        UniqueViolation("23505") {
            override fun createError(details: ServerErrorMessage) = UniqueViolationError(details.constraint!!)
        },
        ForeignKeyViolation("23503") {
            override fun createError(details: ServerErrorMessage) = ForeignKeyViolationError(details.constraint!!)
        },
        CheckViolation("23514") {
            override fun createError(details: ServerErrorMessage) = CheckConstraintViolationError(details.constraint!!)
        },
        ;

        abstract fun createError(details: ServerErrorMessage): DatabaseError
    }

    private fun extractDatabaseError(error: SQLException): DatabaseError {
        val cause = error.cause
        if (cause !is PSQLException) throw error

        val serverError = cause.serverErrorMessage ?: throw error
        val state = PostgresSqlState.entries
            .find { it.code == cause.sqlState }
            ?: throw cause

        return state.createError(serverError)
    }
}
