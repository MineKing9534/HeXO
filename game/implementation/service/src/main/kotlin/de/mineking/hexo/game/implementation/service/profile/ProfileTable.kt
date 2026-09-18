package de.mineking.hexo.game.implementation.service.profile

import de.mineking.hexo.discord.core.discordUserIdColumn
import de.mineking.hexo.game.model.profile.ProfileId
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.datetime.timestamp

object ProfileTable : IdTable<ProfileId>("profiles") {
    override val id = text("id").transform({ ProfileId(it) }, { it.value }).entityId()
    override val primaryKey = PrimaryKey(id)

    val discord = discordUserIdColumn("discord_id")

    val displayName = text("display_name").uniqueIndex()
    val image = text("image_url").nullable()
    val registeredAt = timestamp("registered_at")
}

object ProfileStatisticsTable : IdTable<ProfileId>("profiles") {
    override val id = reference("id", ProfileTable.id, onDelete = ReferenceOption.CASCADE)
    override val primaryKey = PrimaryKey(id)

    val ratingWorldRank = integer("rating_world_rank").nullable()
    val ratingElo = integer("rating_elo")

    val highestRatingWorldRank = integer("highest_rating_world_rank").nullable()
    val highestRatingElo = integer("highest_rating_elo")

    val totalGames = integer("total_games")
    val totalGamesWon = integer("total_games_won")

    val ratedGames = integer("rated_games")
    val ratedGamesWon = integer("rated_games_won")
    val ratedCurrentWinStreak = integer("rated_current_win_streak")
    val ratedLongestWinStreak = integer("rated_longest_win_streak")
}
