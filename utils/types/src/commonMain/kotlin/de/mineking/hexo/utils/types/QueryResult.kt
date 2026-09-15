package de.mineking.hexo.utils.types

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.serialization.Serializable

interface QueryResult<out T> : Flow<T> {
    val totalCount: Int

    companion object {
        val Empty: QueryResult<Nothing> = object : QueryResult<Nothing>, Flow<Nothing> by emptyFlow() {
            override val totalCount = 0
        }
    }
}

@Serializable
data class QueryResultDto<out T>(val entries: List<T>, val totalCount: Int) {
    companion object {
        val Empty = QueryResultDto<Nothing>(emptyList(), 0)
    }
}
