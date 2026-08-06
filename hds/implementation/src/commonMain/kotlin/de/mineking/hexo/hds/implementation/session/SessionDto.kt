package de.mineking.hexo.hds.implementation.session

import de.mineking.hexo.hds.implementation.game.PlayerTile
import de.mineking.hexo.hds.implementation.game.TournamentMatchSnapshotDto
import de.mineking.hexo.hds.model.Instant
import de.mineking.hexo.hds.model.LiveDuration
import de.mineking.hexo.hds.model.TimeControl
import de.mineking.hexo.hds.model.game.GameFinishReason
import de.mineking.hexo.hds.model.game.GameId
import de.mineking.hexo.hds.model.game.GameOptions
import de.mineking.hexo.hds.model.game.PlayerId
import de.mineking.hexo.hds.model.profile.ProfileId
import de.mineking.hexo.hds.model.session.SessionId
import de.mineking.hexo.hds.model.session.SessionPlayerConnectionStatus
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

internal interface ISessionPlayerDto {
    val profileId: ProfileId?
    val displayName: String
    val elo: Int
}

@Serializable
internal data class SessionPlayerDto(
    val id: PlayerId,
    override val profileId: ProfileId?,
    override val displayName: String,
    val rating: Rating,
    val ratingAdjustment: RatingAdjustment?,
    val connection: SessionPlayerConnectionDto,
) : ISessionPlayerDto {
    override val elo = rating.eloScore

    @Serializable
    data class Rating(
        val eloScore: Int,
        val gameCount: Int,
    )

    @Serializable
    data class RatingAdjustment(
        val eloGain: Int,
        val eloLoss: Int,
    )

    @Serializable
    @JsonClassDiscriminator("status")
    @OptIn(ExperimentalSerializationApi::class)
    sealed interface SessionPlayerConnectionDto {
        val status: SessionPlayerConnectionStatus

        @Serializable
        @SerialName("connected")
        object Connected : SessionPlayerConnectionDto {
            override val status = SessionPlayerConnectionStatus.Connected
        }

        @Serializable
        @SerialName("orphaned")
        object Orphaned : SessionPlayerConnectionDto {
            override val status = SessionPlayerConnectionStatus.Orphaned
        }

        @Serializable
        @SerialName("disconnected")
        object Disconnected : SessionPlayerConnectionDto {
            override val status = SessionPlayerConnectionStatus.Disconnected
        }
    }
}

@Serializable
@JsonClassDiscriminator("status")
@OptIn(ExperimentalSerializationApi::class)
internal sealed interface SessionStateDto {
    @Serializable
    @SerialName("lobby")
    data object Lobby : SessionStateDto

    sealed interface GameSessionState : SessionStateDto {
        val gameId: GameId
    }

    @Serializable
    @SerialName("in-game")
    data class InGame(
        override val gameId: GameId,
        val startedAt: Instant,
    ) : GameSessionState

    @Serializable
    @SerialName("finished")
    data class Finished(
        override val gameId: GameId,
        val finishReason: GameFinishReason,
        val winningPlayerId: PlayerId?,
        val rematchAcceptedPlayerIds: List<PlayerId>,
    ) : GameSessionState
}

@Serializable
internal data class SessionDto(
    val id: SessionId,
    val gameOptions: GameOptions,
    val players: List<SessionPlayerDto>,
    val tournament: TournamentMatchSnapshotDto?,
    val state: SessionStateDto,
)

@Serializable
internal data class SessionGameStateDto(
    val cells: List<SessionMoveDto>? = null,
    val playerTiles: Map<PlayerId, PlayerTile>? = null,
    val currentTurnPlayerId: PlayerId?,
    val placementsRemaining: Int,
    val turnCount: Int,
    @SerialName("currentTurnExpiresInMs") val currentTurnExpiresIn: LiveDuration?,
    @SerialName("playerTimeRemainingMs") val playerTimeRemaining: Map<PlayerId, LiveDuration>,
)

@Serializable
internal data class SessionMoveDto(
    val occupiedBy: PlayerId,
    @SerialName("x") val q: Int,
    @SerialName("y") val r: Int,
)

@Serializable
internal data class LobbyPlayerDto(
    override val profileId: ProfileId?,
    override val displayName: String,
    override val elo: Int,
) : ISessionPlayerDto

@Serializable
internal data class LobbyInfoDto(
    val id: SessionId,
    val players: List<LobbyPlayerDto>,
    val timeControl: TimeControl,
    val rated: Boolean,
    val createdAt: Instant,
    val startedAt: Instant?,
)
