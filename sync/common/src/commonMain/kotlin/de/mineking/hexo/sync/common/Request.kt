package de.mineking.hexo.sync.common

import de.mineking.hexo.board.CellCoordinate
import de.mineking.hexo.board.CellHighlight
import de.mineking.hexo.board.LineHighlight
import de.mineking.hexo.hds.session.SessionId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface SessionSyncRequest

@Serializable
@SerialName("navigate")
data class SessionSyncNavigateRequest(
    val sessionId: SessionId?,
) : SessionSyncRequest

@Serializable
@SerialName("update")
data class SessionSyncUpdateRequest(
    val cellHighlights: Map<CellCoordinate, CellHighlight>,
    val lineHighlights: List<LineHighlight>,
) : SessionSyncRequest

@Serializable
@SerialName("cell")
data class SessionSyncCellHighlightRequest(
    val coordinate: CellCoordinate,
    val highlight: CellHighlight?,
) : SessionSyncRequest

@Serializable
@SerialName("line")
data class SessionSyncLineHighlightRequest(
    val line: LineHighlight,
    val remove: Boolean,
) : SessionSyncRequest
