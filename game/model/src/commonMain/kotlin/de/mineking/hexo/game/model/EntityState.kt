package de.mineking.hexo.game.model

sealed interface EntityState<out T> {
    data object Loading : EntityState<Nothing>
    data object NotFound : EntityState<Nothing>
    data class Data<out T>(val value: T) : EntityState<T>
}

class EntityNotFoundException : RuntimeException()
