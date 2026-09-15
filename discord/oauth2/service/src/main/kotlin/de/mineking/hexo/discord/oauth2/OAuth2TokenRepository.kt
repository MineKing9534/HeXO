package de.mineking.hexo.discord.oauth2

import de.mineking.hexo.database.DatabaseManager
import de.mineking.hexo.database.notify
import de.mineking.hexo.database.select
import de.mineking.hexo.database.throwOnDatabaseError
import de.mineking.hexo.discord.core.DiscordUserId
import de.mineking.hexo.discord.oauth2.database.DiscordUserTokensTable
import de.mineking.hexo.discord.oauth2.database.TokenTransform
import de.mineking.hexo.discord.oauth2.model.DiscordTokenStoreDatabaseEvent
import de.mineking.hexo.discord.oauth2.model.OAuth2Flow
import de.mineking.hexo.utils.coroutines.KeyedSemaphore
import de.mineking.hexo.utils.types.IError
import de.mineking.hexo.utils.types.Result
import de.mineking.hexo.utils.types.successIfNotNullOrElse
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.append
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.intLiteral
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob

sealed interface OAuth2TokenQueryError : IError
data object OAuth2TokensNotFound : OAuth2TokenQueryError
data object OAuth2TokenRefreshFailed : OAuth2TokenQueryError

interface OAuth2TokenRepository {
    suspend fun store(tokens: OAuth2Tokens, cause: OAuth2Flow)
    suspend fun revoke(userId: DiscordUserId)

    suspend fun hasTokens(discordUserId: DiscordUserId): Boolean
    suspend fun getUsersWithTokens(requiredScopes: Set<Scope> = emptySet()): List<DiscordUserId>
    suspend fun getUserTokens(discordUserId: DiscordUserId): Result<OAuth2Tokens, OAuth2TokenQueryError>
}

class OAuth2TokenRepositoryImpl(
    val database: DatabaseManager,
    val discordOAuth2Client: DiscordOAuth2Client,
    private val transform: TokenTransform,
) : OAuth2TokenRepository {
    private val tokenLocks = KeyedSemaphore<DiscordUserId>()

    private fun ResultRow.mapToTokens() = OAuth2Tokens(
        client = discordOAuth2Client,
        data = OAuth2TokensDto(
            accessToken = transform.unwrap(this[DiscordUserTokensTable.accessToken].bytes),
            refreshToken = transform.unwrap(this[DiscordUserTokensTable.refreshToken].bytes),
            expiresAt = this[DiscordUserTokensTable.expiresAt],
            scopes = this[DiscordUserTokensTable.scopes].map(Scope::valueOf),
        ),
        id = this[DiscordUserTokensTable.id].value,
    )

    private fun UpdateBuilder<*>.bindTokens(tokens: OAuth2Tokens) {
        this[DiscordUserTokensTable.accessToken] = ExposedBlob(transform.wrap(tokens.data.accessToken))
        this[DiscordUserTokensTable.refreshToken] = ExposedBlob(transform.wrap(tokens.data.refreshToken))
        this[DiscordUserTokensTable.expiresAt] = tokens.data.expiresAt
        this[DiscordUserTokensTable.scopes] = tokens.data.scopes.map(Scope::name)
    }

    override suspend fun store(tokens: OAuth2Tokens, cause: OAuth2Flow) {
        database.transaction(readOnly = false) {
            DiscordUserTokensTable.upsert {
                this[DiscordUserTokensTable.id] = tokens.id
                bindTokens(tokens)
            }.execute()

            notify(DiscordTokenStoreDatabaseEvent(tokens.id, cause))
        }.throwOnDatabaseError()
    }

    override suspend fun hasTokens(discordUserId: DiscordUserId): Boolean {
        return database.transaction(readOnly = true) {
            DiscordUserTokensTable
                .select(intLiteral(0))
                .where(DiscordUserTokensTable.id eq discordUserId)
                .execute()
                .firstOrNull() != null
        }.throwOnDatabaseError()
    }

    override suspend fun getUsersWithTokens(requiredScopes: Set<Scope>): List<DiscordUserId> {
        return database.transaction(readOnly = true) {
            DiscordUserTokensTable
                .select(DiscordUserTokensTable.id)
                .where(DiscordUserTokensTable.scopes.containsAll(requiredScopes.map(Scope::name)))
                .execute()
                .map { it.value }
                .toList()
        }.throwOnDatabaseError()
    }

    override suspend fun getUserTokens(discordUserId: DiscordUserId) = tokenLocks.withPermit(discordUserId) {
        val stored = database.transaction(readOnly = true) {
            DiscordUserTokensTable
                .select()
                .where(DiscordUserTokensTable.id eq discordUserId)
                .execute()
                .firstOrNull()
                ?.let { it.mapToTokens() to it[DiscordUserTokensTable.refreshToken] }
        }.throwOnDatabaseError() ?: return@withPermit Result.Error(OAuth2TokensNotFound)
        val (tokens, refreshToken) = stored

        if (!tokens.isExpired()) return@withPermit Result.Success(tokens)
        val updated = tokens.refresh()

        val storedRefreshToken = DiscordUserTokensTable.refreshToken eq refreshToken
        val tokensUnchanged = (DiscordUserTokensTable.id eq discordUserId) and storedRefreshToken
        val writeApplied = database.transaction(readOnly = false) {
            if (updated == null) {
                DiscordUserTokensTable.delete(where = tokensUnchanged).isNotEmpty()
            } else {
                DiscordUserTokensTable.update(where = tokensUnchanged) {
                    bindTokens(updated)
                }.isNotEmpty()
            }
        }.throwOnDatabaseError()

        if (!writeApplied) {
            return@withPermit database.transaction(readOnly = true) {
                DiscordUserTokensTable
                    .select()
                    .where(DiscordUserTokensTable.id eq discordUserId)
                    .execute()
                    .firstOrNull()
                    ?.mapToTokens()
            }.throwOnDatabaseError().successIfNotNullOrElse(OAuth2TokensNotFound)
        }

        updated.successIfNotNullOrElse(OAuth2TokenRefreshFailed)
    }

    override suspend fun revoke(userId: DiscordUserId) {
        val tokens = database.transaction(readOnly = false) {
            DiscordUserTokensTable.delete(
                returning = DiscordUserTokensTable.columns,
                where = DiscordUserTokensTable.id eq userId,
            ).firstOrNull()?.mapToTokens()
        }.throwOnDatabaseError() ?: return

        tokens.revoke()
    }
}

private fun <T> Column<List<T>>.containsAll(values: Collection<T>) = object : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.append(this@containsAll, " @> ")
        queryBuilder.registerArgument(columnType, values.toList())
        queryBuilder.append("::varchar[]")
    }
}
