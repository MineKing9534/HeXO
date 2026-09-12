package de.mineking.hexo.hds.implementation.tournament

import de.mineking.hexo.game.model.tournament.DoubleEliminationTournamentFormat
import de.mineking.hexo.game.model.tournament.SingleEliminationTournamentFormat
import de.mineking.hexo.game.model.tournament.SwissTournamentFormat
import de.mineking.hexo.game.model.tournament.SwissTournamentRound
import de.mineking.hexo.game.model.tournament.TournamentMatch

internal fun createTournamentFormat(dto: TournamentDto, matches: List<TournamentMatch>) = when (dto.format) {
    TournamentFormatType.Swiss -> SwissTournamentFormatImpl(dto, matches)
    TournamentFormatType.SingleElimination -> SingleEliminationTournamentFormatImpl(matches)
    TournamentFormatType.DoubleElimination -> DoubleEliminationTournamentFormatImpl(matches)
}

private class SwissTournamentFormatImpl(
    dto: TournamentDto,
    override val rawMatches: List<TournamentMatch>,
) : SwissTournamentFormat {
    override val roundCount = dto.swissRoundCount!!
    override val rounds = rawMatches
        .groupBy { it.info.round }
        .mapValues { (round, matches) -> SwissTournamentRound(round, matches.sortedBy { it.info.order }) }
        .values
        .sortedBy { it.round }
        .let { rounds ->
            if (rounds.size >= roundCount) return@let rounds
            rounds + List(roundCount - rounds.size) {
                SwissTournamentRound(
                    round = it + rounds.size,
                    matches = emptyList(),
                )
            }
        }
}

private class SingleEliminationTournamentFormatImpl(override val rawMatches: List<TournamentMatch>) : SingleEliminationTournamentFormat

private class DoubleEliminationTournamentFormatImpl(override val rawMatches: List<TournamentMatch>) : DoubleEliminationTournamentFormat
