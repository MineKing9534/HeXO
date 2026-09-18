package de.mineking.hexo.game.implementation.service.profile

import de.mineking.hexo.database.DatabaseManager
import de.mineking.hexo.database.select
import de.mineking.hexo.database.throwOnDatabaseError
import de.mineking.hexo.game.implementation.protocol.ProfileDto
import de.mineking.hexo.game.implementation.protocol.ProfileGameStatisticsDto
import de.mineking.hexo.game.implementation.protocol.ProfileGameStatisticsWithStreakDto
import de.mineking.hexo.game.implementation.protocol.ProfileRatingDto
import de.mineking.hexo.game.implementation.protocol.ProfileStatisticsDto
import de.mineking.hexo.game.model.profile.ProfileIdentifier
import de.mineking.hexo.game.model.profile.ProfileNotFoundError
import de.mineking.hexo.game.model.profile.ProfileQueryError
import de.mineking.hexo.utils.types.Result
import de.mineking.hexo.utils.types.successIfNotNullOrElse
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.BooleanColumnType
import org.jetbrains.exposed.v1.core.CustomFunction
import org.jetbrains.exposed.v1.core.CustomOperator
import org.jetbrains.exposed.v1.core.FloatColumnType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.leftJoin
import org.jetbrains.exposed.v1.core.stringParam

class ProfileService(private val database: DatabaseManager) {
    private fun ResultRow.mapToStatistics() = ProfileStatisticsDto(
        rating = ProfileRatingDto(
            worldRank = this[ProfileStatisticsTable.ratingWorldRank],
            elo = this[ProfileStatisticsTable.ratingElo],
        ),
        highestRating = ProfileRatingDto(
            worldRank = this[ProfileStatisticsTable.highestRatingWorldRank],
            elo = this[ProfileStatisticsTable.highestRatingElo],
        ),
        totalGames = ProfileGameStatisticsDto(
            total = this[ProfileStatisticsTable.totalGames],
            won = this[ProfileStatisticsTable.totalGamesWon],
        ),
        ratedGames = ProfileGameStatisticsWithStreakDto(
            total = this[ProfileStatisticsTable.ratedGames],
            won = this[ProfileStatisticsTable.ratedGamesWon],
            currentWinStreak = this[ProfileStatisticsTable.ratedCurrentWinStreak],
            longestWinStreak = this[ProfileStatisticsTable.ratedLongestWinStreak],
        ),
    )

    private fun ResultRow.mapToProfile() = ProfileDto(
        id = this[ProfileTable.id].value,
        discord = this[ProfileTable.discord],
        displayName = this[ProfileTable.displayName],
        image = this[ProfileTable.image],
        registeredAt = this[ProfileTable.registeredAt],
        statistics = if (hasValue(ProfileStatisticsTable.id)) mapToStatistics() else null,
    )

    private fun ProfileIdentifier.toCondition() = when (this) {
        is ProfileIdentifier.Id -> ProfileTable.id eq id
        is ProfileIdentifier.Name -> ProfileTable.displayName eq name
    }

    suspend fun getProfile(id: ProfileIdentifier): Result<ProfileDto, ProfileQueryError> {
        return database.transaction(readOnly = true) {
            ProfileTable.leftJoin(ProfileStatisticsTable, onColumn = { ProfileTable.id }, otherColumn = { ProfileStatisticsTable.id })
                .select()
                .where(id.toCondition())
                .execute()
                .firstOrNull()
                ?.mapToProfile()
                .successIfNotNullOrElse(ProfileNotFoundError)
        }.throwOnDatabaseError()
    }

    suspend fun getProfileStatistics(id: ProfileIdentifier): Result<ProfileStatisticsDto, ProfileQueryError> {
        return database.transaction(readOnly = true) {
            ProfileStatisticsTable.leftJoin(ProfileTable, onColumn = { ProfileTable.id }, otherColumn = { ProfileStatisticsTable.id })
                .select(ProfileStatisticsTable.columns)
                .where(id.toCondition())
                .execute()
                .firstOrNull()
                ?.mapToStatistics()
                .successIfNotNullOrElse(ProfileNotFoundError)
        }.throwOnDatabaseError()
    }

    suspend fun searchProfiles(name: String): List<ProfileDto> {
        return database.transaction(readOnly = true) {
            ProfileTable.select()
                .where(CustomOperator("%", BooleanColumnType(), ProfileTable.displayName, stringParam(name)))
                .order(CustomFunction("similarity", FloatColumnType(), ProfileTable.displayName, stringParam(name)) to SortOrder.DESC)
                .limit(10)
                .execute()
                .map { it.mapToProfile() }
                .toList()
        }.throwOnDatabaseError()
    }
}
