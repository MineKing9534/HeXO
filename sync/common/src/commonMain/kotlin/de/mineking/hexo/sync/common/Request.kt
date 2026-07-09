package de.mineking.hexo.sync.common

import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.hds.session.SessionId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface WatchPartyRequest

@Serializable
@SerialName("navigate")
data class WatchPartyNavigateRequest(
    val sessionId: SessionId?,
) : WatchPartyRequest

@Serializable
data class WatchPartyMoveCountRequest(
    val move: Int,
) : WatchPartyRequest

@Serializable
@SerialName("update")
data class WatchPartyUpdateRequest(
    val cellHighlights: Map<CellCoordinate, CellHighlight>,
    val lineHighlights: List<LineHighlight>,
) : WatchPartyRequest

@Serializable
@SerialName("cell")
data class WatchPartyCellHighlightRequest(
    val coordinate: CellCoordinate,
    val highlight: CellHighlight?,
) : WatchPartyRequest

@Serializable
@SerialName("line")
data class WatchPartyLineHighlightRequest(
    val line: LineHighlight,
    val remove: Boolean,
) : WatchPartyRequest
