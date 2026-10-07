package de.mineking.hexo.hds.implementation.socket

import de.mineking.hexo.game.model.session.SessionId
import de.mineking.hexo.game.model.tournament.TournamentId
import de.mineking.hexo.hds.implementation.Instant
import de.mineking.hexo.hds.implementation.session.LobbyInfoDto
import de.mineking.hexo.hds.implementation.session.SessionDto
import de.mineking.hexo.hds.implementation.session.SessionGameStateDto
import de.mineking.hexo.hds.implementation.session.SessionMoveDto
import de.mineking.hexo.hds.implementation.session.SessionPlayerDto
import de.mineking.hexo.hds.implementation.session.SessionStateDto
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SealedSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
sealed interface HdsSocketEvent

@Serializable
@SerialName("tournament-updated")
internal data class TournamentUpdate(
    val tournamentId: TournamentId,
    val updatedAt: Instant,
) : HdsSocketEvent

@Serializable
@SerialName("session-watch-started")
internal data class SessionWatchStarted(
    val session: SessionDto,
    val gameState: SessionGameStateDto,
) : HdsSocketEvent

@Serializable
@SerialName("session-watch-error")
internal data class SessionWatchError(
    val sessionId: SessionId,
    val message: String,
) : HdsSocketEvent

@Serializable
@SerialName("session-updated")
internal data class SessionUpdated(
    val sessionId: SessionId,
    val session: PartialSessionDto,
) : HdsSocketEvent {
    @Serializable
    data class PartialSessionDto(
        val state: SessionStateDto? = null,
        val players: List<SessionPlayerDto>? = null,
    )
}

@Serializable
@SerialName("game-state")
internal data class GameStateUpdated(
    val sessionId: SessionId,
    val gameState: SessionGameStateDto,
) : HdsSocketEvent

@Serializable
@SerialName("game-cell-place")
internal data class GameCellPlace(
    val sessionId: SessionId,
    val state: SessionGameStateDto,
    val cell: SessionMoveDto,
) : HdsSocketEvent

@Serializable
@SerialName("lobby-removed")
internal data class LobbyRemoved(
    val id: SessionId,
) : HdsSocketEvent

@Serializable(with = LobbyUpdated.LobbyUpdateSerializer::class)
internal data class LobbyUpdated(
    val id: SessionId,
    val data: LobbyInfoDto,
) : HdsSocketEvent {
    object LobbyUpdateSerializer : KSerializer<LobbyUpdated> {
        @OptIn(SealedSerializationApi::class)
        override val descriptor = object : SerialDescriptor by LobbyInfoDto.serializer().descriptor {
            override val serialName = "lobby-updated"
        }

        override fun deserialize(decoder: Decoder) = LobbyInfoDto.serializer().deserialize(decoder).let { LobbyUpdated(it.id, it) }
        override fun serialize(encoder: Encoder, value: LobbyUpdated) = throw UnsupportedOperationException()
    }
}
