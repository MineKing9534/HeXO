package de.mineking.hexo.watchparty.model

import de.mineking.hexo.game.model.game.GameId
import de.mineking.hexo.game.model.session.SessionId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface WatchPartyNavigateTarget {
    @Serializable
    @SerialName("sandbox")
    data object Sandbox : WatchPartyNavigateTarget

    @Serializable
    @SerialName("session")
    data class Session(val id: SessionId) : WatchPartyNavigateTarget

    @Serializable
    @SerialName("game")
    data class Game(val id: GameId) : WatchPartyNavigateTarget
}
