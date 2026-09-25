package de.mineking.hexo.game.implementation.service.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import de.mineking.hexo.database.DatabaseManager
import de.mineking.hexo.database.Transaction
import de.mineking.hexo.database.select
import de.mineking.hexo.database.throwOnDatabaseError
import de.mineking.hexo.discord.oauth2.TokenResponse
import de.mineking.hexo.discord.oauth2.UserDetails
import de.mineking.hexo.game.implementation.protocol.AbstractToken
import de.mineking.hexo.game.implementation.protocol.AccessToken
import de.mineking.hexo.game.implementation.protocol.AuthSession
import de.mineking.hexo.game.implementation.protocol.AuthSessionId
import de.mineking.hexo.game.implementation.protocol.AuthSessionRefreshError
import de.mineking.hexo.game.implementation.protocol.AuthSessionRevocationError
import de.mineking.hexo.game.implementation.protocol.AuthSessionValidationError
import de.mineking.hexo.game.implementation.protocol.InvalidTokenError
import de.mineking.hexo.game.implementation.protocol.RefreshMismatchError
import de.mineking.hexo.game.implementation.protocol.RefreshToken
import de.mineking.hexo.game.implementation.service.profile.ProfileStatisticsTable
import de.mineking.hexo.game.implementation.service.profile.ProfileTable
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.utils.types.Result
import de.mineking.hexo.utils.types.isSuccess
import de.mineking.hexo.utils.types.map
import de.mineking.hexo.utils.types.successIfNotNullOrElse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import net.dv8tion.jda.api.utils.DiscordAssets
import net.dv8tion.jda.api.utils.ImageFormat
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.core.leftJoin
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import java.time.Instant
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import kotlin.uuid.Uuid

class AuthSessionManager(
    private val database: DatabaseManager,
    private val algorithm: Algorithm,
    internal val accessTokenTtl: Duration,
    internal val refreshTokenTtl: Duration,
    internal val cookiePath: String,
) {
    internal val refreshCookiePath = "${cookiePath.trimEnd('/')}/auth"

    init {
        require(cookiePath.startsWith('/')) { "Cookie path must be absolute" }
    }

    companion object {
        private const val ISSUER = "HeXO"
    }

    private val verifier = JWT.require(algorithm)
        .withIssuer(ISSUER)
        .build()

    private fun generateToken(
        session: AuthSessionId,
        ttl: Duration,
        config: JWTCreator.Builder.() -> Unit,
    ) = JWT.create()
        .withIssuer(ISSUER)
        .withIssuedAt(Instant.now())
        .withExpiresAt(Instant.now().plus(ttl.toJavaDuration()))
        .withSubject(session.value)
        .apply(config)
        .sign(algorithm)

    private fun generateTokens(sessionId: AuthSessionId, refreshId: String): AuthSession {
        val accessToken = generateToken(sessionId, accessTokenTtl) {
            withClaim("type", "access")
        }

        val refreshToken = generateToken(sessionId, refreshTokenTtl) {
            withClaim("type", "refresh")
            withClaim("rid", refreshId)
        }

        return AuthSession(
            id = sessionId,
            accessToken = AccessToken(accessToken),
            refreshToken = RefreshToken(refreshToken),
        )
    }

    private suspend fun Transaction.createProfile(details: UserDetails): PrincipalId {
        val profileId = ProfileTable.insert(returning = listOf(ProfileTable.id)) {
            this[ProfileTable.displayName] = details.username
            this[ProfileTable.image] = DiscordAssets.userAvatar(
                ImageFormat.PNG,
                details.id.value.toString(),
                details.avatar,
            ).url
        }.first()[ProfileTable.id]

        ProfileStatisticsTable.insert {
            this[ProfileStatisticsTable.id] = profileId
        }.execute()

        val principalId = PrincipalTable.insert(returning = listOf(PrincipalTable.id)) {
            this[PrincipalTable.discordId] = details.id
            this[PrincipalTable.profileId] = profileId
            this[PrincipalTable.type] = PrincipalType.User
        }.first()[PrincipalTable.id]

        return principalId.value
    }

    internal suspend fun createSession(tokens: TokenResponse) = database.transaction(readOnly = false) {
        acquireLock("hexo:principal:discord:${tokens.details.id.value}")

        val existing = PrincipalTable.select(PrincipalTable.id)
            .where(PrincipalTable.discordId eq tokens.details.id)
            .execute()
            .firstOrNull()
            ?.value

        if (existing != null) return@transaction createSession0(existing)

        val created = createProfile(tokens.details)
        createSession0(created)
    }.throwOnDatabaseError()

    private suspend fun Transaction.createSession0(principal: PrincipalId): AuthSession {
        val result = AuthSessionTable.insert(returning = listOf(AuthSessionTable.id, AuthSessionTable.refreshId)) {
            this[AuthSessionTable.principalId] = principal
        }.first()

        val sessionId = result[AuthSessionTable.id].value
        val refreshId = result[AuthSessionTable.refreshId].toString()

        return generateTokens(sessionId, refreshId)
    }

    internal suspend fun createSession(principal: PrincipalId) = database.transaction(readOnly = false) {
        createSession0(principal)
    }.throwOnDatabaseError()

    internal suspend fun validateSession(token: AccessToken): Result<ProfileId, AuthSessionValidationError> {
        return try {
            val decoded = verifier.verify(token.value)
            if (decoded.getClaim("type").asString() != "access") return Result.Error(InvalidTokenError)

            val sessionId = AuthSessionId(decoded.subject)

            database.transaction(readOnly = false) {
                AuthSessionTable.delete(where = AuthSessionTable.lastUsedAt lessEq Clock.System.now() - refreshTokenTtl).execute()

                PrincipalTable.leftJoin(AuthSessionTable, onColumn = { AuthSessionTable.principalId }, otherColumn = { PrincipalTable.id })
                    .select(PrincipalTable.profileId)
                    .where(AuthSessionTable.id eq sessionId)
                    .execute()
                    .map { it.value }
                    .firstOrNull()
                    .successIfNotNullOrElse(InvalidTokenError)
                    .also {
                        if (it.isSuccess()) {
                            AuthSessionTable.update(where = AuthSessionTable.id eq sessionId) {
                                this[AuthSessionTable.lastUsedAt] = CurrentTimestamp
                            }.execute()
                        }
                    }
            }.throwOnDatabaseError()
        } catch (_: JWTVerificationException) {
            Result.Error(InvalidTokenError)
        }
    }

    internal suspend fun refreshSession(token: RefreshToken): Result<AuthSession, AuthSessionRefreshError> {
        return try {
            val decoded = verifier.verify(token.value)
            if (decoded.getClaim("type").asString() != "refresh") return Result.Error(InvalidTokenError)

            val sessionId = AuthSessionId(decoded.subject)
            val refreshId = decoded.getClaim("rid").asString()
                ?.let { runCatching { Uuid.parse(it) }.getOrNull() }
                ?: return Result.Error(InvalidTokenError)

            database.transaction(readOnly = false) {
                AuthSessionTable.update(
                    returning = listOf(AuthSessionTable.id, AuthSessionTable.refreshId),
                    where = (AuthSessionTable.id eq sessionId) and
                        (AuthSessionTable.refreshId eq refreshId) and
                        (AuthSessionTable.lastUsedAt greater Clock.System.now() - refreshTokenTtl),
                ) {
                    this[AuthSessionTable.refreshId] = Uuid.random()
                    this[AuthSessionTable.lastUsedAt] = CurrentTimestamp
                }.firstOrNull()
                    .successIfNotNullOrElse(RefreshMismatchError)
                    .map {
                        generateTokens(
                            sessionId = it[AuthSessionTable.id].value,
                            refreshId = it[AuthSessionTable.refreshId].toString(),
                        )
                    }
                    .also {
                        // In this case either the session doesn't exist at all (=> this operation does nothing) OR the session exists but the
                        // current refreshId does not match the one in the database.
                        // This means, that either an attacker used an old refresh token but the user already used that or an attacker used
                        // a valid refresh token and the user is now trying to use the same one again.
                        // (because they don't know that the token was already used by said attacker)
                        // So we revoke the session in both cases because it was compromised, causing all access and refresh tokens associated with that session to be invalid
                        if (!it.isSuccess()) {
                            val _ = revokeSession(sessionId)
                        }
                    }
            }.throwOnDatabaseError()
        } catch (_: JWTVerificationException) {
            Result.Error(InvalidTokenError)
        }
    }

    suspend fun revokeSession(token: AbstractToken): Result<Unit, AuthSessionRevocationError> {
        return try {
            val decoded = JWT.decode(token.value)
            algorithm.verify(decoded)

            val type = decoded.getClaim("type").asString()
            if (decoded.algorithm != algorithm.name || decoded.issuer != ISSUER || (type != "access" && type != "refresh")) {
                return Result.Error(InvalidTokenError)
            }

            val sessionId = AuthSessionId(decoded.subject)

            database.transaction(readOnly = false) {
                revokeSession(sessionId)
            }.throwOnDatabaseError()
        } catch (_: JWTVerificationException) {
            Result.Error(InvalidTokenError)
        }
    }

    private suspend fun Transaction.revokeSession(sessionId: AuthSessionId) = AuthSessionTable
        .delete(where = AuthSessionTable.id eq sessionId)
        .firstOrNull()
        .successIfNotNullOrElse(InvalidTokenError)
        .map {}
}
