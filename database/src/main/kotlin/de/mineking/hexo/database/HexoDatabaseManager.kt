package de.mineking.hexo.database

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.SqlLogger
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.statements.StatementContext
import org.jetbrains.exposed.v1.core.statements.expandArgs
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import java.util.ServiceLoader
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction as suspendJdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction as jdbcTransaction

internal val logger = KotlinLogging.logger {}

class HexoDatabaseManager(url: String) {
    private val database = Database.connect(url)

    init {
        jdbcTransaction(database) {
            configure()

            ServiceLoader.load(Migration::class.java).forEach {
                logger.info { "Running database migration ${it.javaClass}" }

                it.run {
                    migrate()
                }
            }
        }
    }

    suspend fun <T> transaction(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        suspendJdbcTransaction(database) {
            configure()
            block()
        }
    }

    private fun JdbcTransaction.configure() {
        addLogger(object : SqlLogger {
            override fun log(context: StatementContext, transaction: Transaction) {
                logger.debug { context.expandArgs(this@configure) }
            }
        })
    }
}
