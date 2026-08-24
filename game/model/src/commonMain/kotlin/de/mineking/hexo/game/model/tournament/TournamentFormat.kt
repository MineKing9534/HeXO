package de.mineking.hexo.game.model.tournament

sealed interface TournamentFormat {
    val rawMatches: List<TournamentMatch>
}

data class SwissTournamentRound(val round: Int, val matches: List<TournamentMatch>)

interface SwissTournamentFormat : TournamentFormat {
    val roundCount: Int
    val rounds: List<SwissTournamentRound>
}

interface SingleEliminationTournamentFormat : TournamentFormat {
    // TODO
}

interface DoubleEliminationTournamentFormat : TournamentFormat {
    // TODO
}
