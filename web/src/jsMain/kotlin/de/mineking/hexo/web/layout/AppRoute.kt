package de.mineking.hexo.web.layout

import de.mineking.hexo.game.model.game.GameId
import de.mineking.hexo.game.model.session.SessionId
import de.mineking.hexo.watchparty.common.WatchPartyId

enum class NavBarEntry(val label: String, val route: AppRoute) {
    Sessions("Sessions", AppRoute.SessionList),
    History("Match History", AppRoute.FinishedGameList),
    Sandbox("Sandbox", AppRoute.Sandbox),
    WatchParty("Watch Party", AppRoute.WatchPartyHome),
}

sealed interface AppRoute {
    val href: String
    val navBarEntry: NavBarEntry

    data object SessionList : AppRoute {
        override val href = "/sessions"
        override val navBarEntry get() = NavBarEntry.Sessions
    }

    data class Session(val id: SessionId) : AppRoute {
        override val href = "/sessions/${id.value}"
        override val navBarEntry get() = NavBarEntry.Sessions
    }

    data object FinishedGameList : AppRoute {
        override val href = "/games?page=1&rated=all"
        override val navBarEntry get() = NavBarEntry.History
    }

    data class FinishedGame(val id: GameId) : AppRoute {
        override val href = "/games/${id.value}"
        override val navBarEntry get() = NavBarEntry.History
    }

    data object Sandbox : AppRoute {
        override val href = "/sandbox"
        override val navBarEntry get() = NavBarEntry.Sandbox
    }

    data object WatchPartyHome : AppRoute {
        override val href = "/watchparty"
        override val navBarEntry get() = NavBarEntry.WatchParty
    }

    data class WatchParty(val id: WatchPartyId) : AppRoute {
        override val href = "/watchparty/${id.value}"
        override val navBarEntry get() = NavBarEntry.WatchParty
    }
}
