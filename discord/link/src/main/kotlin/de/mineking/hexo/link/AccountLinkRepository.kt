package de.mineking.hexo.link

import de.mineking.hexo.database.HexoDatabaseManager
import de.mineking.hexo.database.UnexpectedDatabaseErrorException
import de.mineking.hexo.database.UniqueViolationError
import de.mineking.hexo.database.throwOnDatabaseError
import de.mineking.hexo.discord.core.DiscordUserId
import de.mineking.hexo.hds.model.profile.ProfileId
import de.mineking.hexo.link.database.AccountLinkTable
import de.mineking.hexo.utils.types.IError
import de.mineking.hexo.utils.types.Result
import de.mineking.hexo.utils.types.mapError
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.upsert

sealed interface CreateLinkError : IError

object TargetProfileAlreadyLinkedError : CreateLinkError

class AccountLinkRepository(private val database: HexoDatabaseManager) {
    suspend fun getHexoProfile(discordUserId: DiscordUserId): ProfileId? {
        return database.transaction(readOnly = true) {
            AccountLinkTable
                .select(AccountLinkTable.linkedProfileId)
                .where(AccountLinkTable.id eq discordUserId)
                .firstOrNull()
                ?.get(AccountLinkTable.linkedProfileId)
        }.throwOnDatabaseError()
    }

    suspend fun getDiscordProfiles(profileIds: Collection<ProfileId>): Map<ProfileId, DiscordUserId> {
        return database.transaction(readOnly = true) {
            AccountLinkTable
                .select(AccountLinkTable.linkedProfileId, AccountLinkTable.id)
                .where(AccountLinkTable.linkedProfileId inList profileIds)
                .associate { it[AccountLinkTable.linkedProfileId] to it[AccountLinkTable.id].value }
        }.throwOnDatabaseError()
    }

    @IgnorableReturnValue
    suspend fun removeLinkedProfile(discordUserId: DiscordUserId): Boolean {
        return database.transaction(readOnly = false) {
            AccountLinkTable.deleteWhere { AccountLinkTable.id eq discordUserId } > 0
        }.throwOnDatabaseError()
    }

    suspend fun createLink(discordUserId: DiscordUserId, linkedProfileId: ProfileId): Result<Unit, CreateLinkError> {
        return database.transaction(readOnly = false) {
            AccountLinkTable.upsert {
                it[this.id] = discordUserId
                it[this.linkedProfileId] = linkedProfileId
            }

            Unit
        }.mapError {
            when (it) {
                is UniqueViolationError if it.constraintName == "hds_profile_unique_index" -> TargetProfileAlreadyLinkedError
                else -> throw UnexpectedDatabaseErrorException.Known(it)
            }
        }
    }
}

suspend fun AccountLinkRepository.getDiscordProfile(profileId: ProfileId) = getDiscordProfiles(listOf(profileId))[profileId]
