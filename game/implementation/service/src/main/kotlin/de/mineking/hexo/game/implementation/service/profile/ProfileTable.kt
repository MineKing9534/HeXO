package de.mineking.hexo.game.implementation.service.profile

import de.mineking.hexo.database.NanoId
import de.mineking.hexo.database.NanoIdTable
import de.mineking.hexo.game.model.profile.ProfileId
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp

private const val DEFAULT_RATING = 1000

internal object ProfileTable : NanoIdTable<ProfileId>("profiles") {
    override fun NanoId.wrapId() = ProfileId(value)
    override fun ProfileId.unwrapId() = NanoId(value)

    val displayName = text("display_name").uniqueIndex()
    val image = text("image_url").nullable()
    val registeredAt = timestamp("registered_at").defaultExpression(CurrentTimestamp)
}

internal object ProfileStatisticsTable : IdTable<ProfileId>("profile_statistics") {
    override val id = reference("id", ProfileTable.id, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(id)

    val ratingWorldRank = integer("rating_world_rank").nullable()
    val ratingElo = integer("rating_elo").default(DEFAULT_RATING)

    val highestRatingWorldRank = integer("highest_rating_world_rank").nullable()
    val highestRatingElo = integer("highest_rating_elo").default(DEFAULT_RATING)

    val totalGames = integer("total_games").default(0)
    val totalGamesWon = integer("total_games_won").default(0)

    val ratedGames = integer("rated_games").default(0)
    val ratedGamesWon = integer("rated_games_won").default(0)
    val ratedCurrentWinStreak = integer("rated_current_win_streak").default(0)
    val ratedLongestWinStreak = integer("rated_longest_win_streak").default(0)
}
