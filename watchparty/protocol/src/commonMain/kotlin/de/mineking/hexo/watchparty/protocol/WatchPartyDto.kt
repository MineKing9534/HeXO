package de.mineking.hexo.watchparty.protocol

import de.mineking.hexo.board.Board
import de.mineking.hexo.game.model.game.GameId
import de.mineking.hexo.game.model.session.SessionId
import de.mineking.hexo.watchparty.model.WatchPartyId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface WatchPartyTargetDto {
    @Serializable
    @SerialName("session")
    data class Session(
        val sessionId: SessionId,
        val move: Int,
        val overlay: Board,
    ) : WatchPartyTargetDto

    @Serializable
    @SerialName("game")
    data class Game(
        val gameId: GameId,
        val move: Int,
        val overlay: Board,
    ) : WatchPartyTargetDto

    @Serializable
    @SerialName("sandbox")
    data class Sandbox(
        val board: Board,
        val canUndo: Boolean,
        val canRedo: Boolean,
    ) : WatchPartyTargetDto
}

@Serializable
sealed interface WatchPartyResponse

@Serializable
@SerialName("data")
data class WatchPartyDto(
    val id: WatchPartyId,
    val target: WatchPartyTargetDto?,
    val clearableHighlights: Boolean,
    val revision: Long = 0,
    val generation: Long = 0,
) : WatchPartyResponse

@Serializable
@SerialName("closed")
data class WatchPartyClosedResponse(val message: String) : WatchPartyResponse

@Serializable
data class WatchPartyAcknowledgement(val state: WatchPartyDto, val error: String? = null)

@Serializable
data class WatchPartyCreatedResponse(val id: WatchPartyId)
