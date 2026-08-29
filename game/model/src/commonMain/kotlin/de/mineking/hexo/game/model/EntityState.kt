package de.mineking.hexo.game.model

interface EntityId {
    val value: String
}

interface Entity<I : EntityId> {
    val id: I
    val url: String
}

interface EntityRepository<@Suppress("unused") out T : Entity<*>> {
    val url: String
}

fun <I : EntityId> EntityRepository<Entity<I>>.urlOf(id: I) = "$url/${id.value}"

sealed interface EntityState<out T : Entity<*>> {
    data object Loading : EntityState<Nothing>
    data object NotFound : EntityState<Nothing>
    data class Data<out T : Entity<*>>(val value: T) : EntityState<T>
}

class EntityNotFoundException : RuntimeException()
