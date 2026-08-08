package de.mineking.hexo.database

import de.mineking.hexo.utils.types.IError
import de.mineking.hexo.utils.types.Result
import de.mineking.hexo.utils.types.isSuccess
import de.mineking.hexo.utils.types.orThrow
import java.sql.SQLException

sealed interface DatabaseError : IError

abstract class ConstraintViolationError(val constraintName: String) : DatabaseError {
    override fun toString() = "${javaClass.simpleName}[constraintName='$constraintName']"
}

class CheckConstraintViolationError(constraintName: String) : ConstraintViolationError(constraintName)
class UniqueViolationError(constraintName: String) : ConstraintViolationError(constraintName)
class ForeignKeyViolationError(constraintName: String) : ConstraintViolationError(constraintName)

class NotNullViolationError(val columnName: String) : DatabaseError {
    override fun toString() = "NotNullViolationError[columnName='$columnName']"
}

sealed class UnexpectedDatabaseErrorException(message: String) : RuntimeException("Unexpected database error: $message") {
    class Known(val error: DatabaseError) : UnexpectedDatabaseErrorException(error.toString())
    class Unknown(override val cause: SQLException) : UnexpectedDatabaseErrorException(cause.message.toString())
}

fun <T : Any, E : IError> Result<T?, DatabaseError>.mapNullableResult(
    notFoundError: E,
    errorMapper: (DatabaseError) -> E = { throw UnexpectedDatabaseErrorException.Known(it) },
) = when {
    isSuccess() -> {
        if (value != null) {
            Result.Success(value as T)
        } else {
            Result.Error(notFoundError)
        }
    }
    else -> Result.Error(errorMapper(error))
}

fun <T> Result<T, DatabaseError>.throwOnDatabaseError() = orThrow { UnexpectedDatabaseErrorException.Known(it) }
