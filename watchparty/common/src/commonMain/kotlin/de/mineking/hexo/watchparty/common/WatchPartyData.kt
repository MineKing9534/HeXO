package de.mineking.hexo.watchparty.common

import de.mineking.hexo.board.Board
import de.mineking.hexo.game.model.game.GameId
import de.mineking.hexo.game.model.session.SessionId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class WatchPartyId(val value: String)

@Serializable
sealed interface WatchPartyResponse

@Serializable
@SerialName("pong")
data object WatchPartyPongResponse : WatchPartyResponse

@Serializable
sealed interface WatchPartyTarget {
    @Serializable
    @SerialName("session")
    data class Session(
        val sessionId: SessionId,
        val move: Int,
        val overlay: Board,
    ) : WatchPartyTarget

    @Serializable
    @SerialName("game")
    data class Game(
        val gameId: GameId,
        val move: Int,
        val overlay: Board,
    ) : WatchPartyTarget

    @Serializable
    @SerialName("sandbox")
    data class Sandbox(
        val board: Board,
    ) : WatchPartyTarget
}

@Serializable
@SerialName("data")
data class WatchPartyData(
    val id: WatchPartyId,
    val target: WatchPartyTarget?,
    val clearableHighlights: Boolean = false,
) : WatchPartyResponse

@Serializable
@SerialName("error")
data class WatchPartyErrorResponse(val message: String) : WatchPartyResponse
