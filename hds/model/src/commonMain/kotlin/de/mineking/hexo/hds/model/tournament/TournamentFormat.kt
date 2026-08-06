package de.mineking.hexo.hds.model.tournament

sealed interface TournamentFormat {
    val rawMatches: List<TournamentMatch>
}

interface SwissTournamentFormat : TournamentFormat {
    val roundCount: Int
    val rounds: Map<Int, List<TournamentMatch>>
}

interface SingleEliminationTournamentFormat : TournamentFormat {
    // TODO
}

interface DoubleEliminationTournamentFormat : TournamentFormat {
    // TODO
}
