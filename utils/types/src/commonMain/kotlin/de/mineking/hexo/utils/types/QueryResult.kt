package de.mineking.hexo.utils.types

import kotlinx.serialization.Serializable

@Serializable
data class QueryResult<out T>(val entries: List<T>, val totalCount: Int) {
    companion object {
        val Empty = QueryResult<Nothing>(emptyList(), 0)
    }
}
