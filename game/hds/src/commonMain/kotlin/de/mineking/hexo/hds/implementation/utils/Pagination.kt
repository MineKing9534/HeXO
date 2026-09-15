package de.mineking.hexo.hds.implementation.utils

import de.mineking.hexo.hds.implementation.HdsApiClient
import de.mineking.hexo.utils.types.QueryResult
import de.mineking.hexo.utils.types.QueryResultDto
import de.mineking.hexo.utils.types.Selector
import de.mineking.hexo.utils.types.SelectorFilter
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

private const val PAGE_SIZE = 100

data class PaginatedKey<C, E : Any, F : SelectorFilter<E>>(val scope: C, val page: Int, val pageSize: Int, val filter: F?)

fun <C, E : Any, F : SelectorFilter<E>> EntityRequesterFactory.createPaginated(
    client: HdsApiClient,
    path: (C) -> String,
    config: HttpRequestBuilder.(F?) -> Unit = {},
    parse: suspend (HttpResponse) -> QueryResultDto<E>?,
) = PaginatedEntityRequester<C, E, F>(createEntityRequester {
    val response = client.request(path(it.scope)) {
        parameter("page", it.page)
        parameter("pageSize", it.pageSize)
        config(it.filter)
    }

    parse(response)
})

class PaginatedEntityRequester<C, E : Any, F : SelectorFilter<E>>(
    private val delegate: EntityRequester<PaginatedKey<C, E, F>, QueryResultDto<E>?>,
) {
    suspend fun fetchPaginated(scope: C, selector: Selector<E, F>): QueryResult<E> {
        suspend fun fetchPage(page: Int) = delegate.fetch(PaginatedKey(scope, page, PAGE_SIZE, selector.filter))

        val offset = selector.offset ?: 0
        val limit = selector.limit
        val firstPage = offset / PAGE_SIZE + 1

        val (firstEntries, totalCount) = fetchPage(firstPage)
            ?: return QueryResult.Empty

        val lastPage = lastPage(offset, limit ?: Int.MAX_VALUE, totalCount)

        val flow = flow {
            var remaining = limit

            suspend fun emitAll(entries: List<E>) {
                for (entry in entries) {
                    if (remaining == 0) break
                    emit(entry)
                    remaining = remaining?.minus(1)
                }
            }

            emitAll(firstEntries.drop(offset % PAGE_SIZE))

            for (page in (firstPage + 1)..lastPage) {
                if (remaining == 0) break
                emitAll(fetchPage(page)?.entries ?: break)
            }
        }

        return object : QueryResult<E>, Flow<E> by flow {
            override val totalCount = totalCount
        }
    }

    private fun lastPage(offset: Int, limit: Int, totalCount: Int): Int {
        val endExclusive = minOf(offset.toLong() + limit, totalCount.toLong())
        return maxOf(offset / PAGE_SIZE + 1, ((endExclusive + PAGE_SIZE - 1) / PAGE_SIZE).toInt())
    }
}
