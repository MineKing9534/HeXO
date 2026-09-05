package de.mineking.hexo.hds.implementation.session

import de.mineking.hexo.game.model.game.GameId
import de.mineking.hexo.game.model.game.PlayerId
import de.mineking.hexo.game.model.profile.ProfileId
import de.mineking.hexo.game.model.session.SessionId
import de.mineking.hexo.game.model.session.SessionPlayerConnectionStatus
import de.mineking.hexo.hds.implementation.Instant
import de.mineking.hexo.hds.implementation.LiveDuration
import de.mineking.hexo.hds.implementation.TimeControl
import de.mineking.hexo.hds.implementation.game.AbstractPlayerDto
import de.mineking.hexo.hds.implementation.game.GameFinishReasonDto
import de.mineking.hexo.hds.implementation.game.GameOptionsDto
import de.mineking.hexo.hds.implementation.game.PlayerTile
import de.mineking.hexo.hds.implementation.game.TournamentMatchSnapshotDto
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

internal interface AbstractSessionPlayerDto : AbstractPlayerDto {
    override val profileId: ProfileId?
    override val displayName: String
    override val elo: Int
    val connectionStatus: SessionPlayerConnectionStatus
}

@Serializable
internal data class LobbyPlayerDto(
    override val profileId: ProfileId?,
    override val displayName: String,
    override val elo: Int,
) : AbstractSessionPlayerDto {
    override val playerId = PlayerId("")
    override val connectionStatus = SessionPlayerConnectionStatus.Connected
}

@Serializable
internal data class SessionPlayerDto(
    val id: PlayerId,
    override val profileId: ProfileId?,
    override val displayName: String,
    val rating: Rating,
    val ratingAdjustment: RatingAdjustment?,
    val connection: SessionPlayerConnectionDto,
) : AbstractSessionPlayerDto {
    override val playerId = id
    override val elo = rating.eloScore
    override val connectionStatus get() = connection.status

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
    val createdAt: Instant

    @Serializable
    @SerialName("lobby")
    data class Lobby(
        override val createdAt: Instant,
    ) : SessionStateDto

    sealed interface GameSessionState : SessionStateDto {
        val gameId: GameId
        val startedAt: Instant
    }

    @Serializable
    @SerialName("in-game")
    data class InGame(
        override val gameId: GameId,
        override val createdAt: Instant,
        override val startedAt: Instant,
    ) : GameSessionState

    @Serializable
    @SerialName("finished")
    data class Finished(
        override val gameId: GameId,
        override val createdAt: Instant,
        override val startedAt: Instant,
        val finishedAt: Instant,
        val finishReason: GameFinishReasonDto,
        val winningPlayerId: PlayerId?,
        val rematchAcceptedPlayerIds: List<PlayerId>,
    ) : GameSessionState
}

@Serializable
internal data class SessionDto(
    val id: SessionId,
    val gameOptions: GameOptionsDto,
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
internal data class LobbyInfoDto(
    val id: SessionId,
    val players: List<LobbyPlayerDto>,
    val timeControl: TimeControl,
    val rated: Boolean,
    val createdAt: Instant,
    val startedAt: Instant?,
)
