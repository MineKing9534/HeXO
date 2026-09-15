package de.mineking.hexo.database

import de.mineking.hexo.utils.types.Result
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.serializer
import org.jetbrains.exposed.v1.core.ColumnSet
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.FieldSet
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Slice
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.core.statements.UpsertStatement
import java.sql.SQLException
import java.util.ServiceLoader

internal val logger = KotlinLogging.logger {}

interface Transaction {
    val manager: DatabaseManager

    suspend fun acquireLock(key: String)

    fun select(fields: FieldSet): Query<ResultRow>

    fun Table.update(
        where: Op<Boolean>,
        returning: List<Expression<*>> = emptyList(),
        config: UpdateStatement.() -> Unit,
    ): StatementResult

    fun Table.insert(
        returning: List<Expression<*>> = emptyList(),
        config: InsertStatement<*>.() -> Unit,
    ): StatementResult

    fun Table.upsert(
        returning: List<Expression<*>> = emptyList(),
        config: UpsertStatement<*>.() -> Unit,
    ): StatementResult

    suspend fun <T> Table.batchInsert(elements: Collection<T>, config: BatchInsertStatement.(T) -> Unit)

    suspend fun Table.delete(
        returning: List<Expression<*>> = emptyList(),
        where: Op<Boolean>,
    ): StatementResult

    suspend fun <T> notify(type: SerializationStrategy<T>, event: T)

    suspend fun migrate(table: Table)
}

interface StatementResult : Flow<ResultRow> {
    suspend fun isEmpty() = firstOrNull() == null
    suspend fun isNotEmpty() = !isEmpty()

    suspend fun execute() {
        first()
    }
}

fun StatementResult(result: Flow<ResultRow>): StatementResult = object : StatementResult, Flow<ResultRow> by result {}

context(transaction: Transaction)
fun ColumnSet.select(expressions: List<Expression<*>> = this.columns) = transaction.select(Slice(this, expressions))

context(transaction: Transaction)
fun ColumnSet.select(vararg expressions: Expression<*>) = select(expressions.toList())

context(transaction: Transaction)
fun <T> ColumnSet.select(column: ExpressionWithColumnType<T>): Query<T> = select(listOf(column)).map(column.columnType) { it[column] }

interface DatabaseNotificationListener {
    val notificationFormat: StringFormat

    suspend fun <T> listen(type: DeserializationStrategy<T>, handler: suspend (T) -> Unit): Nothing
}

interface DatabaseManager : DatabaseNotificationListener, AutoCloseable {

    suspend fun <T> transaction(
        readOnly: Boolean,
        block: suspend Transaction.() -> T,
    ): Result<T, DatabaseError>
}

suspend inline fun <reified T> Transaction.notify(event: T) {
    notify(manager.notificationFormat.serializersModule.serializer<T>(), event)
}

suspend inline fun <reified T> DatabaseNotificationListener.listen(noinline handler: suspend (T) -> Unit): Nothing {
    listen(notificationFormat.serializersModule.serializer<T>(), handler)
}

suspend fun DatabaseManager.runMigrations() {
    transaction(readOnly = false) {
        try {
            acquireLock("hexo:database:migrations")

            ServiceLoader.load(Migration::class.java).forEach {
                logger.info { "Running database migration ${it::class.qualifiedName}" }

                it.run {
                    migrate()
                }
            }
        } catch (e: SQLException) {
            throw UnexpectedDatabaseErrorException.Unknown(e)
        }
    }.throwOnDatabaseError()
}
