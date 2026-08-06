package de.mineking.hexo.hds.implementation.tournament

import de.mineking.hexo.hds.model.tournament.DoubleEliminationTournamentFormat
import de.mineking.hexo.hds.model.tournament.SingleEliminationTournamentFormat
import de.mineking.hexo.hds.model.tournament.SwissTournamentFormat
import de.mineking.hexo.hds.model.tournament.TournamentMatch

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
        .groupBy { it.round }
        .mapValues { (_, matches) -> matches.sortedBy { it.order } }
        .toMutableMap()
        .also { map ->
            for (round in 1..roundCount) {
                if (round !in map) {
                    map[round] = emptyList()
                }
            }
        }
}

private class SingleEliminationTournamentFormatImpl(override val rawMatches: List<TournamentMatch>) : SingleEliminationTournamentFormat

private class DoubleEliminationTournamentFormatImpl(override val rawMatches: List<TournamentMatch>) : DoubleEliminationTournamentFormat
