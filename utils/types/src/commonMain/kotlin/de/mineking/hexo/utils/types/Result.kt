package de.mineking.hexo.utils.types

import kotlinx.serialization.Serializable
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@Serializable
sealed interface Result<out T, out E : IError> {
    @Serializable
    data class Success<out T>(val value: T) : Result<T, Nothing>

    @Serializable
    data class Error<out E : IError>(val error: E) : Result<Nothing, E>
}

interface IError

@OptIn(ExperimentalContracts::class)
inline fun <T, S, E : IError> Result<T, E>.map(mapper: (T) -> S): Result<S, E> {
    contract {
        callsInPlace(mapper, InvocationKind.AT_MOST_ONCE)
    }

    return when {
        isSuccess() -> Result.Success(mapper(this.value))
        else -> this
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <T, E : IError, F : IError> Result<T, E>.mapError(mapper: (E) -> F): Result<T, F> {
    contract {
        callsInPlace(mapper, InvocationKind.AT_MOST_ONCE)
    }

    return when {
        isSuccess() -> this
        else -> Result.Error(mapper(this.error))
    }
}

fun <T> Result<T, *>.orNull() = orElse { null }

@OptIn(ExperimentalContracts::class)
inline fun <T> Result<T, *>.orElse(other: () -> T): T {
    contract {
        callsInPlace(other, InvocationKind.AT_MOST_ONCE)
    }

    return when {
        isSuccess() -> this.value
        else -> other()
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <T, E : IError> Result<T, E>.orThrow(error: (E) -> Throwable): T {
    contract {
        callsInPlace(error, InvocationKind.AT_MOST_ONCE)
    }

    return when {
        isSuccess() -> this.value
        else -> throw error(this.error)
    }
}

@OptIn(ExperimentalContracts::class)
fun <T, E : IError> Result<T, E>.isSuccess(): Boolean {
    contract {
        returns(true) implies (this@isSuccess is Result.Success<T>)
        returns(false) implies (this@isSuccess is Result.Error<E>)
    }
    return this is Result.Success<T>
}
