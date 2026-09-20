package de.mineking.hexo.database.r2dbc

import de.mineking.hexo.database.DatabaseError
import de.mineking.hexo.database.DatabaseManager
import de.mineking.hexo.database.Transaction
import de.mineking.hexo.database.UnexpectedDatabaseErrorException
import de.mineking.hexo.utils.types.Result
import io.github.oshai.kotlinlogging.KotlinLogging
import io.r2dbc.spi.R2dbcException
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

internal val logger = KotlinLogging.logger {}

abstract class R2dbcDatabaseManager protected constructor(
    protected val database: R2dbcDatabase
) : DatabaseManager {
    protected abstract fun extractDatabaseError(error: R2dbcException): DatabaseError

    override suspend fun <T> transaction(
        readOnly: Boolean,
        block: suspend Transaction.() -> T,
    ): Result<T, DatabaseError> {
        return try {
            suspendTransaction(database, readOnly = readOnly) {
                // Catch exceptions inside the transaction to prevent exposed from retrying and logging the error
                try {
                    val transaction = R2dbcTransaction(this@R2dbcDatabaseManager, this)
                    Result.Success(transaction.block())
                } catch (e: R2dbcException) {
                    // Transform R2dbc errors to the custom DatabaseError model. If the exception does not match any
                    // of the expected errors, the exception is rethrown to be handled by exposed.
                    // Otherwise, an UnexpectedDatabaseError is thrown to be caught outside the transaction and transformed into a Result.
                    // This is currently the only way to go about error handling in exposed.
                    throw UnexpectedDatabaseErrorException.Known(extractDatabaseError(e))
                }
            }
        } catch (e: UnexpectedDatabaseErrorException.Known) {
            Result.Error(e.error)
        } catch (e: R2dbcException) {
            throw UnexpectedDatabaseErrorException.Unknown(e)
        }
    }
}
