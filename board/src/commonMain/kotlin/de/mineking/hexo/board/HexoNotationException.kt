package de.mineking.hexo.board

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

open class HexoNotationException(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message)

class HexoNotationFormatException(message: String, cause: Throwable? = null) : HexoNotationException(message, cause)

@OptIn(ExperimentalContracts::class)
inline fun requireHexo(value: Boolean, notationCheck: Boolean = false, lazyMessage: () -> Any) {
    contract {
        returns() implies value
    }
    if (!value) {
        val message = lazyMessage().toString()
        if (notationCheck) {
            throw HexoNotationFormatException(message)
        } else {
            throw HexoNotationException(message)
        }
    }
}
