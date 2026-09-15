package de.mineking.hexo.link

import de.mineking.hexo.database.DatabaseManager
import de.mineking.hexo.database.UnexpectedDatabaseErrorException
import de.mineking.hexo.database.UniqueViolationError
import de.mineking.hexo.database.select
import de.mineking.hexo.database.throwOnDatabaseError
import de.mineking.hexo.discord.core.DiscordUserId
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.link.database.AccountLinkTable
import de.mineking.hexo.utils.types.IError
import de.mineking.hexo.utils.types.Result
import de.mineking.hexo.utils.types.mapError
import kotlinx.coroutines.flow.associate
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList

sealed interface CreateLinkError : IError
object TargetProfileAlreadyLinkedError : CreateLinkError

interface AccountLinkRepository {
    suspend fun getHexoProfile(discordUserId: DiscordUserId): ProfileId?
    suspend fun getDiscordProfiles(profileIds: Collection<ProfileId>): Map<ProfileId, DiscordUserId>

    @IgnorableReturnValue
    suspend fun removeLinkedProfile(discordUserId: DiscordUserId): Boolean

    suspend fun createLink(discordUserId: DiscordUserId, linkedProfileId: ProfileId): Result<Unit, CreateLinkError>
}

suspend fun AccountLinkRepository.getDiscordProfile(profileId: ProfileId) = getDiscordProfiles(listOf(profileId))[profileId]

class AccountLinkRepositoryImpl(private val database: DatabaseManager) : AccountLinkRepository {
    override suspend fun getHexoProfile(discordUserId: DiscordUserId): ProfileId? {
        return database.transaction(readOnly = true) {
            AccountLinkTable
                .select(AccountLinkTable.linkedProfileId)
                .where(AccountLinkTable.id eq discordUserId)
                .execute()
                .firstOrNull()
        }.throwOnDatabaseError()
    }

    override suspend fun getDiscordProfiles(profileIds: Collection<ProfileId>): Map<ProfileId, DiscordUserId> {
        return database.transaction(readOnly = true) {
            AccountLinkTable
                .select(AccountLinkTable.linkedProfileId, AccountLinkTable.id)
                .where(AccountLinkTable.linkedProfileId inList profileIds)
                .execute()
                .associate { it[AccountLinkTable.linkedProfileId] to it[AccountLinkTable.id].value }
        }.throwOnDatabaseError()
    }

    @IgnorableReturnValue
    override suspend fun removeLinkedProfile(discordUserId: DiscordUserId): Boolean {
        return database.transaction(readOnly = false) {
            AccountLinkTable.delete(where = AccountLinkTable.id eq discordUserId)
                .isNotEmpty()
        }.throwOnDatabaseError()
    }

    override suspend fun createLink(discordUserId: DiscordUserId, linkedProfileId: ProfileId): Result<Unit, CreateLinkError> {
        return database.transaction(readOnly = false) {
            AccountLinkTable.upsert {
                this[AccountLinkTable.id] = discordUserId
                this[AccountLinkTable.linkedProfileId] = linkedProfileId
            }.execute()

            Unit
        }.mapError {
            when (it) {
                is UniqueViolationError if it.constraintName == "hds_profile_unique_index" -> TargetProfileAlreadyLinkedError
                else -> throw UnexpectedDatabaseErrorException.Known(it)
            }
        }
    }
}
