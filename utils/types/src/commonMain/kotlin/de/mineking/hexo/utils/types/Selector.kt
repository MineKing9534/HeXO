package de.mineking.hexo.utils.types

interface SelectorFilter<in T : Any>

open class Selector<in T : Any, out F : SelectorFilter<T>> protected constructor(
    val offset: Int?,
    val limit: Int?,
    val filter: F?,
) {
    init {
        require(offset == null || offset >= 0)
        require(limit == null || limit > 0)
    }

    internal fun <R : T> copy(
        offset: Int? = this.offset,
        limit: Int? = this.limit,
    ) = Selector(offset, limit, filter)

    internal fun <R : T, G : SelectorFilter<R>> copy(filter: G) = Selector(offset, limit, filter)

    override fun equals(other: Any?) =
        other is Selector<*, *> && other.offset == offset && other.limit == limit && other.filter == filter

    override fun hashCode(): Int {
        var result = offset.hashCode()
        result = 31 * result + limit.hashCode()
        result = 31 * result + filter.hashCode()
        return result
    }

    operator fun component1() = offset
    operator fun component2() = limit
    operator fun component3() = filter

    companion object Default : Selector<Any, Nothing>(
        offset = null,
        limit = null,
        filter = null,
    )
}

fun <T : Any, F : SelectorFilter<T>> Selector<T, F>.offset(offset: Int?) = copy<T>(offset = offset)
fun <T : Any, F : SelectorFilter<T>> Selector<T, F>.limit(limit: Int?) = copy<T>(limit = limit)
fun <T : Any, R : T, F : SelectorFilter<R>> Selector<T, *>.filter(filter: F) = copy(filter)
fun <T : Any, F : SelectorFilter<T>> Selector<T, F>.adjustFilter(
    constructor: () -> F,
    update: (F) -> F,
) = copy(update(filter ?: constructor()))

fun <T : Any, F : SelectorFilter<T>> Selector<T, F>.range(range: IntRange) = copy<T>(offset = range.first, limit = range.last - range.first + 1)

fun <T : Any, F : SelectorFilter<T>> Selector<T, F>.page(page: Int, pageSize: Int): Selector<T, F> {
    require(page > 0) { "page must be positive." }
    require(pageSize > 0) { "pageSize must be positive." }

    return copy<T>(
        offset = (page - 1) * pageSize,
        limit = pageSize,
    )
}
