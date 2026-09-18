package de.mineking.hexo.database.r2dbc

import de.mineking.hexo.database.DatabaseManager
import de.mineking.hexo.database.StatementResult
import de.mineking.hexo.database.Transaction
import kotlinx.serialization.SerializationStrategy
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.FieldSet
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.intLiteral
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.core.statements.Statement
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.core.statements.UpsertStatement
import org.jetbrains.exposed.v1.core.stringLiteral
import org.jetbrains.exposed.v1.core.stringParam
import org.jetbrains.exposed.v1.migration.r2dbc.MigrationUtils
import org.jetbrains.exposed.v1.r2dbc.batchInsert
import org.jetbrains.exposed.v1.r2dbc.deleteReturning
import org.jetbrains.exposed.v1.r2dbc.insertReturning
import org.jetbrains.exposed.v1.r2dbc.statements.SuspendExecutable
import org.jetbrains.exposed.v1.r2dbc.statements.api.R2dbcPreparedStatementApi
import org.jetbrains.exposed.v1.r2dbc.statements.api.R2dbcResult
import org.jetbrains.exposed.v1.r2dbc.updateReturning
import org.jetbrains.exposed.v1.r2dbc.upsertReturning
import org.jetbrains.exposed.v1.core.Transaction as ExposedTransaction
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction as ExposedR2dbcTransaction

class R2dbcTransaction(
    override val manager: DatabaseManager,
    val exposed: ExposedR2dbcTransaction,
) : Transaction {
    override suspend fun acquireLock(key: String) {
        exposed.exec(AdvisoryLockExecutable(AdvisoryLockStatement(stringParam(key))))
            ?.collect()
    }

    override suspend fun exec(statement: String) = exposed.exec(statement)

    override fun select(fields: FieldSet) = R2dbcQuery(fields, _columnType = null, transform = { it })

    override fun Table.update(
        where: Op<Boolean>,
        returning: List<Expression<*>>,
        config: (UpdateStatement) -> Unit
    ) = StatementResult(
        updateReturning(returning.ifEmpty { listOf(intLiteral(0)) }, where = { where }) {
            config(it)
        }
    )

    override fun Table.insert(returning: List<Expression<*>>, config: (InsertStatement<*>) -> Unit) = StatementResult(
        insertReturning(returning.ifEmpty { listOf(intLiteral(0)) }) {
            config(it)
        }
    )

    override fun Table.upsert(
        returning: List<Expression<*>>,
        config: UpsertStatement<*>.() -> Unit,
    ) = StatementResult(
        upsertReturning(returning = returning.ifEmpty { listOf(intLiteral(0)) }) {
            config(it)
        }
    )

    override suspend fun <T> Table.batchInsert(elements: Collection<T>, config: BatchInsertStatement.(T) -> Unit) {
        batchInsert(elements, shouldReturnGeneratedValues = false, body = config)
    }

    override suspend fun Table.delete(returning: List<Expression<*>>, where: Op<Boolean>) = StatementResult(
        deleteReturning(returning = returning.ifEmpty { listOf(intLiteral(0)) }, where = { where })
    )

    override suspend fun <T> notify(type: SerializationStrategy<T>, event: T) {
        exposed.exec(NotifyExecutable(NotifyStatement(
            channel = stringLiteral(type.descriptor.serialName),
            payload = stringParam(manager.notificationFormat.encodeToString(type, event)),
        )))?.collect()
    }

    override suspend fun migrate(table: Table) {
        val statements = MigrationUtils.statementsRequiredForDatabaseMigration(table, withLogs = false)

        if (statements.isEmpty()) {
            logger.debug { "No migration required" }
        } else {
            statements.forEach {
                exposed.exec(it)
            }

            exposed.db.dialectMetadata.resetCaches()
        }
    }
}

private class AdvisoryLockStatement(
    private val key: Expression<String>,
) : Statement<R2dbcResult>(StatementType.OTHER, emptyList()) {
    override fun arguments() = QueryBuilder(true).run {
        key.toQueryBuilder(this)
        listOf(args)
    }

    override fun prepareSQL(transaction: ExposedTransaction, prepared: Boolean) = QueryBuilder(prepared).apply {
        append("SELECT pg_advisory_xact_lock(hashtextextended(")
        key.toQueryBuilder(this)
        append(", 0))")
    }.toString()
}

private class AdvisoryLockExecutable(
    override val statement: AdvisoryLockStatement,
) : SuspendExecutable<R2dbcResult, AdvisoryLockStatement> {
    override suspend fun R2dbcPreparedStatementApi.executeInternal(transaction: ExposedR2dbcTransaction) = executeQuery()
}

private class NotifyStatement(
    private val channel: Expression<String>,
    private val payload: Expression<String>
) : Statement<R2dbcResult>(StatementType.OTHER, emptyList()) {
    override fun arguments() = QueryBuilder(true).run {
        channel.toQueryBuilder(this)
        payload.toQueryBuilder(this)

        listOf(args)
    }

    override fun prepareSQL(transaction: ExposedTransaction, prepared: Boolean): String = QueryBuilder(prepared).apply {
        append("SELECT pg_notify(")
        channel.toQueryBuilder(this)
        append(", ")
        payload.toQueryBuilder(this)
        append(")")
    }.toString()
}

private class NotifyExecutable(override val statement: NotifyStatement) : SuspendExecutable<R2dbcResult, NotifyStatement> {
    override suspend fun R2dbcPreparedStatementApi.executeInternal(transaction: ExposedR2dbcTransaction) = executeQuery()
}
