package de.mineking.hexo.hds.implementation.socket

import de.mineking.hexo.hds.implementation.session.LobbyInfoDto
import de.mineking.hexo.hds.implementation.session.SessionDto
import de.mineking.hexo.hds.implementation.session.SessionGameStateDto
import de.mineking.hexo.hds.implementation.session.SessionMoveDto
import de.mineking.hexo.hds.implementation.session.SessionPlayerDto
import de.mineking.hexo.hds.implementation.session.SessionStateDto
import de.mineking.hexo.hds.model.Instant
import de.mineking.hexo.hds.model.session.SessionId
import de.mineking.hexo.hds.model.tournament.TournamentId
import de.mineking.hexo.hds.socket.SocketEventRegistry
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.KClass

internal annotation class SocketEventName(val name: String)

sealed interface SocketEvent

sealed interface HexoSocketEvent : SocketEvent

@Serializable
@SocketEventName("tournament-updated")
internal data class TournamentUpdate(
    val tournamentId: TournamentId,
    val updatedAt: Instant,
) : HexoSocketEvent

@Serializable
@SocketEventName("session-watch-started")
internal data class SessionWatchStarted(
    val session: SessionDto,
    val gameState: SessionGameStateDto,
) : HexoSocketEvent

@Serializable
@SocketEventName("session-watch-error")
internal data class SessionWatchError(
    val sessionId: SessionId,
    val message: String,
) : HexoSocketEvent

@Serializable
@SocketEventName("session-updated")
internal data class SessionUpdated(
    val sessionId: SessionId,
    val session: PartialSessionDto,
) : HexoSocketEvent {
    @Serializable
    data class PartialSessionDto(
        val state: SessionStateDto? = null,
        val players: List<SessionPlayerDto>? = null,
    )
}

@Serializable
@SocketEventName("game-state")
internal data class GameStateUpdated(
    val sessionId: SessionId,
    val gameState: SessionGameStateDto,
) : HexoSocketEvent

@Serializable
@SocketEventName("game-cell-place")
internal data class GameCellPlace(
    val sessionId: SessionId,
    val state: SessionGameStateDto,
    val cell: SessionMoveDto,
) : HexoSocketEvent

@Serializable
@SocketEventName("lobby-removed")
internal data class LobbyRemoved(
    val id: SessionId,
) : HexoSocketEvent

@Serializable(with = LobbyUpdated.LobbyUpdateSerializer::class)
@SocketEventName("lobby-updated")
internal data class LobbyUpdated(
    val id: SessionId,
    val data: LobbyInfoDto,
) : HexoSocketEvent {
    object LobbyUpdateSerializer : KSerializer<LobbyUpdated> {
        override val descriptor = LobbyInfoDto.serializer().descriptor

        override fun deserialize(decoder: Decoder) = LobbyInfoDto.serializer().deserialize(decoder).let { LobbyUpdated(it.id, it) }
        override fun serialize(encoder: Encoder, value: LobbyUpdated) = throw UnsupportedOperationException()
    }
}

sealed interface ProtocolSocketEvent : SocketEvent {
    @Serializable
    @SocketEventName("connect")
    data object Connected : ProtocolSocketEvent

    @Serializable
    @SocketEventName("initialized")
    data object Initialized : ProtocolSocketEvent

    @Serializable
    @SocketEventName("disconnect")
    data class Disconnected(val message: String? = null) : ProtocolSocketEvent

    @Serializable
    @SocketEventName("reconnect")
    data object Reconnected : ProtocolSocketEvent

    @Serializable
    @SocketEventName("error")
    data class Error(val message: String) : ProtocolSocketEvent

    @Serializable
    @SocketEventName("connect_error")
    data class ConnectError(val message: String) : ProtocolSocketEvent
}

val KClass<out SocketEvent>.eventName get() = SocketEventRegistry.eventNames[this]
    ?: error("eventName for $this not found, this should never happen")
