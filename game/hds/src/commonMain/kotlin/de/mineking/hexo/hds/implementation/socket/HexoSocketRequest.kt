package de.mineking.hexo.hds.implementation.socket

import de.mineking.hexo.game.model.session.SessionId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface HexoSocketRequest {
    @Serializable
    @SerialName("watch-session")
    data class WatchSession(val sessionId: SessionId) : HexoSocketRequest

    @Serializable
    @SerialName("unwatch-session")
    data class UnwatchSession(val sessionId: SessionId) : HexoSocketRequest
}
