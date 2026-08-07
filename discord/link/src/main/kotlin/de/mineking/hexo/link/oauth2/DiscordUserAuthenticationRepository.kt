package de.mineking.hexo.link.oauth2

import de.mineking.hexo.database.HexoDatabaseManager
import de.mineking.hexo.link.DiscordUserId
import de.mineking.hexo.link.database.DiscordUserTokensTable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.intLiteral
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob
import org.jetbrains.exposed.v1.jdbc.deleteReturning
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.upsert

class DiscordUserAuthenticationRepository(
    val database: HexoDatabaseManager,
    val discordOAuth2Client: DiscordOAuth2Client,
    private val transform: TokenTransform,
) {
    private fun ResultRow.mapToTokens() = OAuth2Tokens(
        client = discordOAuth2Client,
        data = OAuth2TokensDto(
            accessToken = transform.unwrap(this[DiscordUserTokensTable.accessToken].bytes),
            refreshToken = transform.unwrap(this[DiscordUserTokensTable.refreshToken].bytes),
            expiresAt = this[DiscordUserTokensTable.expiresAt],
            scopes = this[DiscordUserTokensTable.scopes],
        ),
        id = this[DiscordUserTokensTable.id].value,
    )

    private fun UpdateBuilder<*>.bindTokens(tokens: OAuth2Tokens) {
        this[DiscordUserTokensTable.accessToken] = ExposedBlob(transform.wrap(tokens.data.accessToken))
        this[DiscordUserTokensTable.refreshToken] = ExposedBlob(transform.wrap(tokens.data.refreshToken))
        this[DiscordUserTokensTable.expiresAt] = tokens.data.expiresAt
        this[DiscordUserTokensTable.scopes] = tokens.data.scopes
    }

    suspend fun authenticateUser(code: String): OAuth2Tokens? {
        val token = discordOAuth2Client.getUserTokens(code) ?: return null
        require(Scope.Identify in token.data.scopes)

        database.transaction {
            val _ = DiscordUserTokensTable.upsert {
                it[this.id] = token.id
                it.bindTokens(token)
            }
        }

        return token
    }

    suspend fun getAuthenticationStatus(discordUserId: DiscordUserId) = database.transaction {
        DiscordUserTokensTable
            .select(intLiteral(0))
            .where(DiscordUserTokensTable.id eq discordUserId)
            .firstOrNull() != null
    }

    suspend fun getUserTokens(discordUserId: DiscordUserId): OAuth2Tokens? {
        val tokens = database.transaction {
            DiscordUserTokensTable
                .selectAll()
                .where(DiscordUserTokensTable.id eq discordUserId)
                .firstOrNull()
                ?.mapToTokens()
        }

        if (tokens?.isExpired() != true) return tokens
        val updated = tokens.refresh()

        return database.transaction {
            if (updated == null) {
                DiscordUserTokensTable.deleteWhere { DiscordUserTokensTable.id eq discordUserId }
                null
            } else {
                DiscordUserTokensTable.update(where = { DiscordUserTokensTable.id eq discordUserId }) {
                    it.bindTokens(updated)
                }

                updated
            }
        }
    }

    @IgnorableReturnValue
    suspend fun removeUser(userId: DiscordUserId): Boolean {
        val tokens = database.transaction {
            DiscordUserTokensTable.deleteReturning(
                returning = DiscordUserTokensTable.columns,
                where = { DiscordUserTokensTable.id eq userId },
            ).firstOrNull()?.mapToTokens()
        } ?: return false

        tokens.revoke()
        return true
    }
}
