package de.mineking.hexo.database

import kotlinx.coroutines.flow.Flow
import org.jetbrains.exposed.v1.core.AbstractQuery
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ExpressionWithColumnType
import org.jetbrains.exposed.v1.core.IColumnType
import org.jetbrains.exposed.v1.core.SortOrder

abstract class Query<T> : ExpressionWithColumnType<T>() {
    abstract fun offset(offset: Int?): Query<T>
    abstract fun limit(limit: Int?): Query<T>

    abstract fun order(order: Pair<Expression<*>, SortOrder>?): Query<T>
    abstract fun where(filter: Expression<Boolean>?): Query<T>

    abstract fun <R> map(type: IColumnType<R & Any>? = null, mapper: (T) -> R): Query<R>

    abstract fun forUpdate(): Query<T>

    abstract suspend fun count(): Int

    abstract fun createQuery(): AbstractQuery<*>
    abstract fun execute(): Flow<T>
}
