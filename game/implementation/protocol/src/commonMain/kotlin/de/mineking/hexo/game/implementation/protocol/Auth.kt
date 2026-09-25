package de.mineking.hexo.game.implementation.protocol

import de.mineking.hexo.utils.types.IError
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class AuthSessionId(val value: String)

@Serializable
data class AuthSession(
    val id: AuthSessionId,
    val accessToken: AccessToken,
    val refreshToken: RefreshToken,
)

@Serializable
data class AuthSessionRevokeRequest(val token: AbstractToken)

@Serializable
data class AuthSessionRefreshRequest(val refreshToken: RefreshToken)

@Serializable
sealed interface AbstractToken {
    val value: String
}

@JvmInline
@Serializable
value class AccessToken(override val value: String) : AbstractToken

@JvmInline
@Serializable
value class RefreshToken(override val value: String) : AbstractToken

@Serializable
sealed interface AuthSessionValidationError : IError

@Serializable
sealed interface AuthSessionRefreshError : IError

@Serializable
sealed interface AuthSessionRevocationError : IError

@Serializable
data object InvalidTokenError : AuthSessionValidationError, AuthSessionRefreshError, AuthSessionRevocationError

@Serializable
data object RefreshMismatchError : AuthSessionRefreshError
