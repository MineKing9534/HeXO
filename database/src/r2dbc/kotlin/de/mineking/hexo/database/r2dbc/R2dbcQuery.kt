package de.mineking.hexo.database.r2dbc

import de.mineking.hexo.database.Query
import kotlinx.coroutines.flow.map
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.FieldSet
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.InternalApi
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.andIfNotNull
import org.jetbrains.exposed.v1.core.vendors.ForUpdateOption
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.Query as ExposedR2dbcQuery

data class R2dbcQuery<T>(
    private val set: FieldSet,
    @Suppress("ConstructorParameterNaming")
    private val _columnType: IColumnType<T & Any>?,
    private val limit: UInt? = null,
    private val offset: UInt? = null,
    private val filter: Op<Boolean>? = null,
    private val order: Pair<Expression<*>, SortOrder>? = null,
    private val forUpdate: Boolean = false,
    private val transform: (ResultRow) -> T,
) : Query<T>() {
    override val columnType: IColumnType<T & Any>
        get() = _columnType ?: error("")

    override fun offset(offset: UInt?) = copy(offset = offset)
    override fun limit(limit: UInt?) = copy(limit = limit)

    override fun order(order: Pair<Expression<*>, SortOrder>?) = copy(order = order)
    override fun where(filter: Op<Boolean>?): R2dbcQuery<T> {
        return copy(filter = if (this.filter == null) filter else this.filter andIfNotNull filter)
    }

    @OptIn(InternalApi::class)
    override fun createQuery() = ExposedR2dbcQuery(set, filter).apply {
        if (this@R2dbcQuery.limit != null) limit(this@R2dbcQuery.limit.toInt())
        if (this@R2dbcQuery.offset != null) offset(this@R2dbcQuery.offset.toLong())
        if (this@R2dbcQuery.order != null) orderBy(this@R2dbcQuery.order)
    }.forUpdate(if (forUpdate) ForUpdateOption.ForUpdate else ForUpdateOption.NoForUpdateOption)

    override fun <R> map(type: IColumnType<R & Any>?, mapper: (T) -> R) = R2dbcQuery(
        set = set,
        _columnType = type,
        limit = limit,
        offset = offset,
        filter = filter,
        order = order,
        forUpdate = forUpdate,
    ) { mapper(transform(it)) }

    override fun forUpdate() = R2dbcQuery(
        set = set,
        _columnType = _columnType,
        limit = limit,
        offset = offset,
        filter = filter,
        order = order,
        forUpdate = true,
        transform = transform,
    )

    override suspend fun count() = ExposedR2dbcQuery(set, filter).count().toUInt()
    override fun execute() = createQuery().map { transform(it) }

    override fun toQueryBuilder(queryBuilder: QueryBuilder) = queryBuilder {
        Table().selectAll()
        append("(")
        createQuery().prepareSQL(this)
        append(")")
    }
}
