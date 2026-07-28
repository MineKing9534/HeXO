package de.mineking.hexo.web.layout

import de.mineking.hexo.hds.session.SessionId
import de.mineking.hexo.sync.common.WatchPartyId

enum class NavBarEntry(val label: String, val route: AppRoute) {
    Lobbies("Lobbies", AppRoute.LobbyList),
    Sandbox("Sandbox", AppRoute.Sandbox),
    WatchParty("Watch Party", AppRoute.WatchPartyHome),
}

sealed interface AppRoute {
    val href: String
    val navBarEntry: NavBarEntry

    data object LobbyList : AppRoute {
        override val href = "/sessions"
        override val navBarEntry get() = NavBarEntry.Lobbies
    }

    data class Session(val id: SessionId) : AppRoute {
        override val href = "/sessions/${id.value}"
        override val navBarEntry get() = NavBarEntry.Lobbies
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
